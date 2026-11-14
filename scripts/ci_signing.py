#!/usr/bin/env python3
"""App Store signing for CI, done through the App Store Connect API.

Creates an Apple Distribution certificate and an App Store provisioning profile for the
app, installs both on the build machine, and writes the profile name for xcodebuild.
No Mac or registered iPhone needed.

Usage:
  ci_signing.py setup     (needs env: ASC_KEY_ID, ASC_ISSUER_ID, KEY_PATH, BUNDLE_ID, OUT_DIR)
  ci_signing.py cleanup   (deletes the profile this run created)

To stay under Apple's limit on distribution certificates, setup revokes older ones and keeps
only the newest before creating a new one. Builds already uploaded are not affected.
"""
import base64
import json
import os
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.request

import jwt
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

API = "https://api.appstoreconnect.apple.com/v1"


def token() -> str:
    with open(os.environ["KEY_PATH"]) as f:
        key = f.read()
    now = int(time.time())
    return jwt.encode(
        {"iss": os.environ["ASC_ISSUER_ID"], "iat": now, "exp": now + 900, "aud": "appstoreconnect-v1"},
        key,
        algorithm="ES256",
        headers={"kid": os.environ["ASC_KEY_ID"], "typ": "JWT"},
    )


def call(method: str, path: str, body: dict | None = None) -> dict:
    request = urllib.request.Request(
        API + path,
        method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={"Authorization": f"Bearer {token()}", "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request) as response:
            raw = response.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as error:
        detail = error.read().decode(errors="replace")
        print(f"::error::App Store Connect API {method} {path} failed ({error.code}): {detail}")
        sys.exit(1)


def run(*args: str) -> None:
    subprocess.run(args, check=True)


def setup() -> None:
    out = os.environ["OUT_DIR"]
    bundle_id = os.environ["BUNDLE_ID"]
    os.makedirs(out, exist_ok=True)

    # 1. Find the registered app ID.
    found = call("GET", f"/bundleIds?filter[identifier]={bundle_id}&limit=200")["data"]
    matches = [b for b in found if b["attributes"]["identifier"] == bundle_id]
    if not matches:
        print(f"::error::App ID {bundle_id} isn't registered in Certificates, Identifiers & Profiles.")
        sys.exit(1)
    bundle_resource_id = matches[0]["id"]

    # 2. Keep at most one older distribution certificate around.
    certs = call("GET", "/certificates?filter[certificateType]=DISTRIBUTION&limit=200")["data"]
    certs.sort(key=lambda c: c["attributes"].get("expirationDate") or "")
    for old in certs[:-1] if len(certs) >= 2 else []:
        print(f"Revoking older distribution certificate {old['id']}")
        call("DELETE", f"/certificates/{old['id']}")

    # 3. New key + certificate signing request, then a fresh Apple Distribution certificate.
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    csr = (
        x509.CertificateSigningRequestBuilder()
        .subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, os.environ.get("CERT_NAME", "Trivia Night CI"))]))
        .sign(key, hashes.SHA256())
    )
    cert_response = call("POST", "/certificates", {
        "data": {
            "type": "certificates",
            "attributes": {
                "certificateType": "DISTRIBUTION",
                "csrContent": csr.public_bytes(serialization.Encoding.PEM).decode(),
            },
        }
    })["data"]
    cert = x509.load_der_x509_certificate(base64.b64decode(cert_response["attributes"]["certificateContent"]))

    # 4. App Store provisioning profile tied to that certificate.
    profile_name = f"{os.environ.get('CERT_NAME', 'Trivia Night CI')} {os.environ.get('GITHUB_RUN_NUMBER', int(time.time()))}"
    profile = call("POST", "/profiles", {
        "data": {
            "type": "profiles",
            "attributes": {"name": profile_name, "profileType": "IOS_APP_STORE"},
            "relationships": {
                "bundleId": {"data": {"type": "bundleIds", "id": bundle_resource_id}},
                "certificates": {"data": [{"type": "certificates", "id": cert_response["id"]}]},
            },
        }
    })["data"]
    profile_bytes = base64.b64decode(profile["attributes"]["profileContent"])
    uuid = profile["attributes"]["uuid"]

    home = os.path.expanduser("~")
    for folder in ("Library/MobileDevice/Provisioning Profiles",
                   "Library/Developer/Xcode/UserData/Provisioning Profiles"):
        path = os.path.join(home, folder)
        os.makedirs(path, exist_ok=True)
        with open(os.path.join(path, f"{uuid}.mobileprovision"), "wb") as f:
            f.write(profile_bytes)

    # 5. Put the certificate and key in a temporary keychain.
    p12_password = secrets.token_hex(16)
    encryption = (
        serialization.PrivateFormat.PKCS12.encryption_builder()
        .kdf_rounds(50000)
        .key_cert_algorithm(pkcs12.PBES.PBESv1SHA1And3KeyTripleDESCBC)
        .hmac_hash(hashes.SHA1())
        .build(p12_password.encode())
    )
    p12_path = os.path.join(out, "dist.p12")
    with open(p12_path, "wb") as f:
        f.write(pkcs12.serialize_key_and_certificates(os.environ.get("CERT_NAME", "Trivia Night CI").encode(), key, cert, None, encryption))

    keychain = os.path.join(out, "ci.keychain-db")
    keychain_password = secrets.token_hex(16)
    run("security", "create-keychain", "-p", keychain_password, keychain)
    run("security", "set-keychain-settings", "-lut", "21600", keychain)
    run("security", "unlock-keychain", "-p", keychain_password, keychain)
    run("security", "import", p12_path, "-P", p12_password, "-A", "-t", "cert", "-f", "pkcs12", "-k", keychain)
    run("security", "set-key-partition-list", "-S", "apple-tool:,apple:", "-s", "-k", keychain_password, keychain)
    existing = subprocess.run(["security", "list-keychains", "-d", "user"], capture_output=True, text=True).stdout
    others = [line.strip().strip('"') for line in existing.splitlines() if line.strip()]
    run("security", "list-keychains", "-d", "user", "-s", keychain, *others)
    os.remove(p12_path)

    with open(os.path.join(out, "state.json"), "w") as f:
        json.dump({"profile_id": profile["id"], "keychain": keychain}, f)
    with open(os.environ["GITHUB_ENV"], "a") as f:
        f.write(f"PROFILE_NAME={profile_name}\n")
    print(f"Signing ready: profile '{profile_name}', certificate {cert_response['id']}.")


def cleanup() -> None:
    state_path = os.path.join(os.environ["OUT_DIR"], "state.json")
    if not os.path.exists(state_path):
        return
    with open(state_path) as f:
        state = json.load(f)
    subprocess.run(["security", "delete-keychain", state["keychain"]], check=False)
    if os.path.exists(os.environ.get("KEY_PATH", "")):
        call("DELETE", f"/profiles/{state['profile_id']}")
    print("Cleaned up signing files.")


if __name__ == "__main__":
    {"setup": setup, "cleanup": cleanup}[sys.argv[1]]()

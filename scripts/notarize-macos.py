#!/usr/bin/env python3
"""Notarize and staple one signed app or DMG; never publishes a release."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile


def run(*args):
    subprocess.run(args, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", type=Path)
    parser.add_argument("--profile", default="pgp-notary")
    parser.add_argument("--keychain")
    parser.add_argument("--timeout", default="10m")
    parser.add_argument("--submission-id", help="Resume an existing submission without uploading again")
    parser.add_argument("--logs", type=Path, default=Path("desktopApp/build/notarization"))
    args = parser.parse_args()
    target = args.target.resolve(strict=True)
    if target.suffix not in (".app", ".dmg"):
        parser.error("target must be a signed .app or .dmg")
    args.logs.mkdir(parents=True, exist_ok=True)
    run("codesign", "--verify", "--deep", "--strict", str(target))
    # Unlike signature verification alone, this rejects an ad-hoc-signed input.
    details = subprocess.run(["codesign", "-d", "--verbose=4", str(target)],
                             check=True, capture_output=True, text=True).stderr
    if "Authority=Developer ID Application:" not in details or "Timestamp=" not in details:
        raise SystemExit("Developer ID Application signature and secure timestamp are required")
    auth = ["--keychain-profile", args.profile]
    if args.keychain:
        auth += ["--keychain", args.keychain]
    submission_id = args.submission_id
    with tempfile.TemporaryDirectory(prefix="pgp-notary-") as temporary:
        upload = target
        if target.suffix == ".app" and not submission_id:
            upload = Path(temporary) / "application.zip"
            run("ditto", "-c", "-k", "--keepParent", str(target), str(upload))
        if not submission_id:
            submitted = subprocess.run(["xcrun", "notarytool", "submit", str(upload), *auth,
                                        "--output-format", "json"], check=True,
                                       capture_output=True, text=True)
            (args.logs / (target.name + ".upload.json")).write_text(submitted.stdout)
            submission_id = json.loads(submitted.stdout)["id"]
        print("Submission ID: " + submission_id, flush=True)
        result = subprocess.run(["xcrun", "notarytool", "wait", submission_id, *auth,
                                 "--timeout", args.timeout, "--output-format", "json"],
                                capture_output=True, text=True)
        log = args.logs / (target.name + ".submission.json")
        log.write_text(result.stdout)
        if result.returncode:
            print(result.stderr)
            raise SystemExit("Notarization did not complete; inspect " + str(log)
                             + " and resume with --submission-id " + submission_id)
        status = json.loads(result.stdout)
        if status.get("status") != "Accepted":
            raise SystemExit("Notarization was not Accepted; submission: " + str(status.get("id")))
    run("xcrun", "stapler", "staple", str(target))
    run("xcrun", "stapler", "validate", str(target))
    run("codesign", "--verify", "--deep", "--strict", str(target))
    if target.suffix == ".app":
        run("spctl", "--assess", "--type", "execute", "--verbose=2", str(target))
    else:
        run("spctl", "--assess", "--type", "open", "--context", "context:primary-signature",
            "--verbose=2", str(target))


if __name__ == "__main__":
    main()

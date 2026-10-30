#!/usr/bin/env python3
"""Select iOS CI from the complete Git diff; uncertainty must run the tests."""

import json
import os
from pathlib import Path
import re
import subprocess


IOS_PREFIXES = (
    "iosApp/", "core/", "hostInput/", "playerShared/",
    "gradle/", "buildSrc/", "build-logic/", ".github/", "docs/licenses/",
)
IOS_FILES = {
    "gradlew", "gradlew.bat", "gradle.properties", "LICENSE", "THIRD_PARTY_NOTICES.md",
    "docs/guides/PLAYER_HELP_IOS.md", "docs/guides/PLAYER_PRIVACY_POLICY_IOS.md",
    "docs/guides/PLAYER_STUDIO_GUIDE.md",
    "scripts/ci-ios-changes.py", "scripts/tests/test_ci_ios_changes.py",
}


def affects_ios(path):
    # All shared-module changes are included conservatively. Gradle configures
    # every project, so even another module's build script may affect this build.
    return (path.startswith(IOS_PREFIXES) or path in IOS_FILES
            or path.endswith((".gradle", ".gradle.kts")))


def changed_paths(event_name, event):
    if event_name == "pull_request":
        base = event["pull_request"]["base"]["sha"]
        head = event["pull_request"]["head"]["sha"]
        revisions = [f"{base}...{head}"]
    elif event_name == "push":
        base, head = event["before"], event["after"]
        revisions = [base, head]
    else:
        raise ValueError("Manual or unknown event: run iOS")
    for sha in (base, head):
        if not re.fullmatch(r"[0-9a-fA-F]{40}", sha) or int(sha, 16) == 0:
            raise ValueError("Missing comparison commit: run iOS")
    # Disable rename detection to inspect BOTH the removed and added paths.
    # NUL delimiters preserve spaces, Unicode and newlines in file names.
    result = subprocess.run(
        ["git", "diff", "--no-renames", "--name-only", "-z", *revisions, "--"],
        check=True, capture_output=True, timeout=30,
    )
    return [os.fsdecode(path) for path in result.stdout.split(b"\0") if path]


def main():
    try:
        event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
        paths = changed_paths(os.environ["GITHUB_EVENT_NAME"], event)
        run_ios = any(affects_ios(path) for path in paths)
        reason = f"{len(paths)} changed paths; iOS inputs {'changed' if run_ios else 'unchanged'}"
    except (OSError, KeyError, TypeError, ValueError, subprocess.SubprocessError):
        run_ios = True
        reason = "Unable to establish complete diff; run iOS conservatively"
    value = str(run_ios).lower()
    print(f"ios={value}: {reason}")
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"ios={value}\n")


if __name__ == "__main__":
    main()

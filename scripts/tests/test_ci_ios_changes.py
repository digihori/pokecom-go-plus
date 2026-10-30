import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("ci_ios_changes", Path(__file__).parents[1] / "ci-ios-changes.py")
FILTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(FILTER)


class IosInputsTest(unittest.TestCase):
    def test_ios_shared_build_and_bundled_documents_are_inputs(self):
        for path in (
            "iosApp/test-simulator.sh", "iosApp/PokecomGOPlayer/PlayerHost.swift",
            "iosApp/PokecomGOPlayerTests/PlayerHostTests.swift", "iosApp/Assets/icon.png",
            "core/src/commonMain/Session.kt", "hostInput/src/commonMain/Keys.kt",
            "playerShared/src/iosMain/Bridge.kt", "core/src/commonTest/SessionTest.kt",
            "gradle/libs.versions.toml", "gradle/wrapper/gradle-wrapper.properties",
            "gradlew", "gradlew.bat", "gradle.properties", "settings.gradle.kts",
            "build.gradle.kts", "desktopApp/build.gradle.kts", "buildSrc/plugin.kt",
            "build-logic/settings.gradle.kts", ".github/workflows/ci.yml",
            "LICENSE", "THIRD_PARTY_NOTICES.md", "docs/licenses/dependency.txt",
            "docs/guides/PLAYER_HELP_IOS.md", "docs/guides/PLAYER_PRIVACY_POLICY_IOS.md",
            "docs/guides/PLAYER_STUDIO_GUIDE.md", "scripts/ci-ios-changes.py",
            "scripts/tests/test_ci_ios_changes.py",
        ):
            with self.subTest(path=path):
                self.assertTrue(FILTER.affects_ios(path))

    def test_desktop_android_and_unbundled_docs_do_not_run_ios(self):
        for path in (
            "desktopApp/src/main/kotlin/Main.kt", "desktopApp/src/test/kotlin/Test.kt",
            "desktopApp/icons/icon.png", "androidApp/src/main/AndroidManifest.xml",
            "controllerUi/src/commonMain/Controller.kt", "README.md", "README.ja.md",
            "AGENTS.md", "docs/development/DEBUGGER_MANUAL_TEST_CASES.md",
            "docs/guides/DEBUGGER.md", "docs/releases/app-store/notes.txt",
            "scripts/inspect-msi.ps1", "tools/memory/pgp_memory.py",
        ):
            with self.subTest(path=path):
                self.assertFalse(FILTER.affects_ios(path))


class GitDiffTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.previous = Path.cwd()
        os.chdir(self.temporary.name)
        self.addCleanup(os.chdir, self.previous)
        self.git("init", "-q")
        self.git("config", "user.email", "ci-test@example.invalid")
        self.git("config", "user.name", "CI Test")
        self.write("iosApp/main.swift", "original")
        self.write("README.md", "original")
        self.base = self.commit()

    def git(self, *args):
        return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout.strip()

    def write(self, path, contents):
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents)

    def commit(self):
        self.git("add", ".")
        self.git("commit", "-qm", "test change")
        return self.git("rev-parse", "HEAD")

    def push(self, head):
        return FILTER.changed_paths("push", {"before": self.base, "after": head})

    def decision(self, event_name, event):
        event_path = Path("event.json").resolve()
        output = Path("output.txt").resolve()
        event_path.write_text(json.dumps(event))
        with patch.dict(os.environ, GITHUB_EVENT_NAME=event_name, GITHUB_EVENT_PATH=str(event_path), GITHUB_OUTPUT=str(output)):
            FILTER.main()
        return output.read_text().splitlines()[-1]

    def test_push_covers_all_commits_not_just_last_commit(self):
        self.write("iosApp/main.swift", "changed")
        self.commit()
        self.write("README.md", "docs only in last commit")
        paths = self.push(self.commit())
        self.assertIn("iosApp/main.swift", paths)
        self.assertTrue(any(map(FILTER.affects_ios, paths)))

    def test_pr_uses_merge_base_and_excludes_base_only_changes(self):
        self.write("README.md", "feature docs")
        head = self.commit()
        self.git("checkout", "-qb", "new-base", self.base)
        self.write("iosApp/main.swift", "base branch change")
        base = self.commit()
        paths = FILTER.changed_paths("pull_request", {"pull_request": {"base": {"sha": base}, "head": {"sha": head}}})
        self.assertEqual(["README.md"], paths)

    def test_rename_out_of_ios_includes_deleted_path(self):
        self.git("mv", "iosApp/main.swift", "moved.txt")
        paths = self.push(self.commit())
        self.assertIn("iosApp/main.swift", paths)
        self.assertIn("moved.txt", paths)
        self.assertTrue(any(map(FILTER.affects_ios, paths)))

    def test_rename_into_ios_includes_added_path(self):
        self.git("mv", "README.md", "iosApp/new.txt")
        self.assertIn("iosApp/new.txt", self.push(self.commit()))

    def test_more_than_300_paths_and_non_ascii_whitespace_names(self):
        for number in range(305):
            self.write(f"docs/{number}.md", "docs")
        unusual = "docs/空白 と\n改行.md"
        self.write(unusual, "docs")
        self.write("iosApp/main.swift", "changed")
        paths = self.push(self.commit())
        self.assertEqual(307, len(paths))
        self.assertIn(unusual, paths)
        self.assertIn("iosApp/main.swift", paths)

    def test_docs_only_writes_false_and_shared_change_writes_true(self):
        self.write("README.md", "docs")
        head = self.commit()
        self.assertEqual("ios=false", self.decision("push", {"before": self.base, "after": head}))
        self.write("core/src/commonMain/Session.kt", "source")
        head = self.commit()
        self.assertEqual("ios=true", self.decision("push", {"before": self.base, "after": head}))

    def test_manual_new_branch_and_missing_history_run_conservatively(self):
        head = self.git("rev-parse", "HEAD")
        for event_name, event in (
            ("workflow_dispatch", {}),
            ("push", {"before": "0" * 40, "after": head}),
            ("push", {"before": "f" * 40, "after": head}),
            ("pull_request", {}),
        ):
            with self.subTest(event=event):
                self.assertEqual("ios=true", self.decision(event_name, event))


if __name__ == "__main__":
    unittest.main()

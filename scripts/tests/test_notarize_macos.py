import json
from pathlib import Path
import runpy
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


main = runpy.run_path(str(Path(__file__).parents[1] / "notarize-macos.py"))["main"]


class NotarizationGateTest(unittest.TestCase):
    def exercise(self, status="Accepted", signature=True, returncode=0, resume=False):
        calls = []

        def command(argv, **kwargs):
            calls.append(tuple(argv))
            details = "Authority=Developer ID Application: Test\nTimestamp=Test\n" if signature else "Signature=adhoc\n"
            if "submit" in argv:
                return subprocess.CompletedProcess(argv, 0, json.dumps({"id": "test-id"}), "")
            if "wait" in argv:
                return subprocess.CompletedProcess(argv, returncode, json.dumps({"status": status, "id": "test-id"}), "")
            return subprocess.CompletedProcess(argv, 0, "", details)

        with tempfile.TemporaryDirectory() as temporary:
            target = Path(temporary) / "Test.app"
            target.mkdir()
            args = ["notarize-macos.py", str(target), "--logs", str(Path(temporary) / "logs")]
            if resume:
                args += ["--submission-id", "test-id"]
            with patch.object(sys, "argv", args), patch("subprocess.run", side_effect=command):
                try:
                    main()
                except SystemExit:
                    return calls, False
        return calls, True

    def test_invalid_and_pending_submissions_never_staple_or_assess(self):
        for status in ("Invalid", "Rejected", "In Progress"):
            with self.subTest(status=status):
                calls, succeeded = self.exercise(status=status)
                self.assertFalse(succeeded)
                self.assertFalse(any("stapler" in c or c[0] == "spctl" for c in calls))

    def test_timeout_never_staples(self):
        calls, succeeded = self.exercise(returncode=124)
        self.assertFalse(succeeded)
        self.assertFalse(any("stapler" in c for c in calls))

    def test_adhoc_signature_never_reaches_apple(self):
        calls, succeeded = self.exercise(signature=False)
        self.assertFalse(succeeded)
        self.assertFalse(any("submit" in c for c in calls))

    def test_accepted_submission_staples_validates_and_assesses(self):
        calls, succeeded = self.exercise()
        self.assertTrue(succeeded)
        self.assertTrue(any("stapler" in c and "staple" in c for c in calls))
        self.assertTrue(any("stapler" in c and "validate" in c for c in calls))
        self.assertEqual("spctl", calls[-1][0])

    def test_resume_does_not_upload_the_same_build_again(self):
        calls, succeeded = self.exercise(resume=True)
        self.assertTrue(succeeded)
        self.assertFalse(any("submit" in c or c[0] == "ditto" for c in calls))


if __name__ == "__main__":
    unittest.main()

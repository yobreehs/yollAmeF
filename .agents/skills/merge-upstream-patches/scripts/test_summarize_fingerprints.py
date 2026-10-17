"""Check JSON coverage and isolated failure decisions without Gradle or APKs."""

from contextlib import redirect_stdout
import io
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

import summarize_fingerprints as reader


class ReportTests(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "results.json"
        self.apks = []

    def add(self, apk_id, status, *, reason="", package="io.github.nexalloy.morphe.youtube.demo",
            fingerprint="Alpha", group_error=None):
        self.apks.append(dict(
            apk=f"/binaries/youtube-{apk_id}.apk", app="youtube", version=str(apk_id),
            groups=[dict(package_name=package, completed=True, error=group_error, results=[dict(
                fingerprint=fingerprint, status=status, reason=reason,
                matches=[f"Ltarget{apk_id};->run()V"] if status == "matched" else [])])]))

    def write(self, complete=True):
        self.path.write_text(json.dumps(dict(schema_version=1, complete=complete, apks=self.apks)))

    def results(self):
        self.write()
        return reader.classify(*reader.read_report(self.path))

    def test_single_failure_requires_an_actual_pass(self):
        self.add(1, "failed")
        self.add(2, "skipped", reason="version")
        self.assertEqual(sum(r.status == "failed" for r in self.results()), 1)
        self.add(3, "matched")
        self.assertEqual([r.status for r in self.results()], ["skipped", "matched"])

    def test_skip_test_is_not_evidence_for_suppression(self):
        self.add(1, "failed")
        self.add(2, "matched")
        self.add(3, "skipped", reason="skip_test")
        self.assertTrue(any(r.status == "failed" for r in self.results()))

    def test_second_failure_is_retained(self):
        self.add(1, "failed")
        self.add(2, "failed")
        self.add(3, "matched")
        self.assertEqual(sum(r.status == "failed" for r in self.results()), 2)

    def test_missing_observation_prevents_suppression(self):
        self.add(1, "failed")
        self.add(2, "matched")
        self.add(3, "matched", fingerprint="Beta")
        rows = self.results()
        self.assertTrue(any(r.fingerprint == "Alpha" and r.status == "failed" for r in rows))
        self.assertTrue(any(r.fingerprint == "Alpha" and r.status == "unobserved" for r in rows))

    def test_null_result_prevents_suppression(self):
        self.add(1, "failed")
        self.add(2, "matched")
        self.add(3, "unvalidated")
        self.assertTrue(any(r.status == "failed" for r in self.results()))

    def test_full_package_names_separate_shared_groups(self):
        self.add(1, "matched", package="io.github.nexalloy.morphe.shared")
        self.add(1, "matched", package="io.github.nexalloy.morphe.youtube.shared")
        self.assertEqual(len({r.group_id for r in self.results()}), 2)

    def test_unidentified_group_failure_is_retained(self):
        self.add(1, "matched", group_error="Discovery failed")
        self.add(2, "matched")
        self.assertTrue(any(r.fingerprint == "<test group>" and r.status == "failed"
                            for r in self.results()))

    def test_incomplete_run_is_rejected(self):
        self.add(1, "matched")
        self.write(complete=False)
        with self.assertRaises(reader.ReportError):
            reader.read_report(self.path)

    def test_filtering_happens_after_classification(self):
        self.add(1, "failed")
        self.add(2, "matched")
        self.write()
        output = io.StringIO()
        with redirect_stdout(output):
            status = reader.main([str(self.path), "--apk", "*-1.apk", "--format", "json"])
        self.assertEqual(status, 0)
        self.assertEqual(json.loads(output.getvalue()), {"counts": {}, "results": []})


if __name__ == "__main__":
    unittest.main()

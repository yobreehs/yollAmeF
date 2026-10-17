#!/usr/bin/env python3
"""Filter and summarize the JSON emitted by FingerprintsKtTest."""

import argparse
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass, field
from fnmatch import fnmatchcase
import json
from pathlib import Path
import sys


class ReportError(ValueError):
    pass


@dataclass
class Result:
    app: str
    apk: str
    version: str
    group: str
    group_id: str
    fingerprint: str
    status: str
    matches: list[str] = field(default_factory=list)
    duration_ms: int | None = None
    reason: str = ""
    error: str = ""


def read_report(path):
    report = json.loads(path.read_text(encoding="utf-8"))
    if report.get("schema_version") != 1 or report.get("complete") is not True:
        raise ReportError("Unsupported or incomplete report; inspect the Gradle run")
    rows = []
    groups = defaultdict(set)
    seen = set()
    for apk in report["apks"]:
        for group in apk["groups"]:
            package = group["package_name"]
            key = (apk["app"], apk["apk"], package)
            if key in seen or not group["completed"]:
                raise ReportError(f"Duplicate or incomplete group: {key}")
            seen.add(key)
            groups[(apk["app"], package)].add(apk["apk"])
            common = dict(app=apk["app"], apk=apk["apk"], version=apk["version"],
                          group=".".join(package.split(".")[5:]) or "shared", group_id=package)
            for item in group["results"]:
                status = item["status"]
                matches = item.get("matches", [])
                if status not in ("matched", "skipped", "failed", "unvalidated"):
                    raise ReportError(f"Unknown status: {status}")
                if not isinstance(matches, list) or (status == "matched" and not matches):
                    raise ReportError(f"Missing matches: {item['fingerprint']}")
                rows.append(Result(**common, fingerprint=item["fingerprint"], status=status,
                                   matches=matches, duration_ms=item.get("duration_ms"),
                                   reason=item.get("reason", ""), error=item.get("error", "")))
            if group.get("error"):
                rows.append(Result(**common, fingerprint="<test group>", status="failed",
                                   error=group["error"]))
            elif not group["results"]:
                rows.append(Result(**common, fingerprint="<test group>", status="unobserved"))
    if not rows:
        raise ReportError("No fingerprint results found")
    return rows, groups


def classify(rows, groups):
    by_fingerprint = defaultdict(dict)
    apks = defaultdict(set)
    versions = {row.apk: row.version for row in rows}
    for row in rows:
        key = (row.app, row.group_id, row.fingerprint)
        if row.apk in by_fingerprint[key]:
            raise ReportError(f"Duplicate fingerprint: {row.apk}: {row.group_id}: {row.fingerprint}")
        by_fingerprint[key][row.apk] = row
    for (app, _), tested in groups.items():
        apks[app].update(tested)

    output = []
    for (app, package, fingerprint), executions in sorted(by_fingerprint.items()):
        example = next(iter(executions.values()))
        if fingerprint != "<test group>":
            for apk in apks[app] - executions.keys():
                executions[apk] = Result(app, apk, versions[apk], example.group, package,
                                         fingerprint, "unobserved")
        failures = [row for row in executions.values() if row.status == "failed"]
        isolated = (
            fingerprint != "<test group>" and len(failures) == 1
            and any(row.status == "matched" for row in executions.values())
            and all(row.status in ("matched", "failed") or
                    (row.status == "skipped" and row.reason in ("version", "target_app"))
                    for row in executions.values())
        )
        output.extend(row for row in executions.values() if not (isolated and row.status == "failed"))
    return sorted(output, key=lambda row: (row.app, row.group_id, row.fingerprint, row.apk))


def selected(value, patterns):
    return not patterns or any(fnmatchcase(value, pattern) for pattern in patterns)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", nargs="?", type=Path,
                        default=Path(__file__).resolve().parents[4] /
                        "app/build/reports/fingerprints/testDebugUnitTest.json")
    parser.add_argument("--group", action="append", help="Short group or full package name glob")
    parser.add_argument("--fingerprint", action="append", help="Fingerprint/finder name glob")
    parser.add_argument("--apk", action="append", help="APK path or basename glob")
    parser.add_argument("--details", action="store_true", help="Include matches and skips in text output")
    parser.add_argument("--format", choices=("text", "json"), default="text")
    args = parser.parse_args(argv)

    def included(row):
        return ((selected(row.group, args.group) or selected(row.group_id, args.group))
                and selected(row.fingerprint, args.fingerprint)
                and (selected(row.apk, args.apk) or selected(Path(row.apk).name, args.apk)))

    try:
        raw, groups = read_report(args.report)
        classified = classify(raw, groups)
        if not any(included(row) for row in raw + classified):
            raise ReportError("No results for these filters; no match has been validated")
        rows = [row for row in classified if included(row)]
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.exit(2, f"error: {error}\n")

    counts = dict(sorted(Counter(row.status for row in rows).items()))
    if args.format == "json":
        print(json.dumps({"counts": counts, "results": [asdict(row) for row in rows]}, indent=2))
    else:
        print("Fingerprint results: " + (", ".join(f"{key}={value}" for key, value in counts.items())
                                         or "no retained records"))
        for row in rows:
            if args.details or row.status in ("failed", "unvalidated"):
                suffix = f" -> {', '.join(row.matches)}" if row.matches else ""
                if row.reason:
                    suffix += f" ({row.reason})"
                print(f"{row.status}: {Path(row.apk).name} | {row.group_id} | {row.fingerprint}{suffix}")
    return int(any(row.status in ("failed", "unvalidated") for row in rows))


if __name__ == "__main__":
    sys.exit(main())

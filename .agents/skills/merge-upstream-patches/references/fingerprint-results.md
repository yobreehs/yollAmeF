# Reading fingerprint test results

`FingerprintsKtTest` directly writes structured JSON while preserving its console and JUnit results. Run:

```sh
./gradlew :app:testDebugUnitTest --tests io.github.nexalloy.FingerprintsKtTest
```

The report is `app/build/reports/fingerprints/testDebugUnitTest.json`. Other Gradle test tasks use their task name as the filename. Read this JSON directly, or use the standard-library Python helper for filtering and the skill's isolated failure rule:

```sh
python .agents/skills/merge-upstream-patches/scripts/summarize_fingerprints.py
python .agents/skills/merge-upstream-patches/scripts/summarize_fingerprints.py --group 'layout.*' --details
python .agents/skills/merge-upstream-patches/scripts/summarize_fingerprints.py --fingerprint '*Fingerprint' --apk '*21.41.*' --format json
```

The optional positional argument selects another JSON report. `--group`, `--fingerprint`, and `--apk` accept case-sensitive glob patterns and can be repeated. Groups can be selected by their short display name or full package name. Finder getter names retain the test output's initial capital letter. `--details` includes matched members and skip reasons; JSON output always includes individual records.

## JSON contents

The versioned report records run timestamps, completion, APK paths, app names, actual manifest versions, and test groups identified by their full Kotlin package. Each group records its completion, unexpected group errors, and individual fingerprint/finder results. Each result includes its name, status, matched members as a list, execution time, skip reason, and exception details when applicable. `matches` uses Dalvik descriptors for DexKit classes, fields, and methods, falling back to string representations for other values. Console output retains the original readable member signatures.

- `matched`: the member actually executed and returned a non-null result. Lists must be nonempty. Match count is the length of `matches`; a match does not prove the intended injection point.
- `skipped`: the member was skipped with reason `skip_test`, `target_app`, or `version`. No skip counts as a pass.
- `unvalidated`: an execution returned `null`.
- `failed`: an execution raised an exception. Unexpected discovery or group setup failures are recorded separately on the group.

The helper adds `unobserved` when a fingerprint recorded for one APK is absent from another APK of the same app, or when a group contains no member results. An undiscovered member is not a pass. Full package names distinguish groups that share the same display name.

## Failure and coverage rules

The raw JSON preserves actual outcomes. The helper applies the isolated failure rule before filtering: a named fingerprint's single failure is silently omitted when at least one other APK actually matches it and every other relevant execution passes. Explicit version/app skips are outside that fingerprint's applicability and are excluded from this decision; they never count as passes. `skip_test`, missing observations, null results, a second failure, or lack of any actual pass prevents suppression. Unidentified group failures are never suppressed. Do not include accepted isolated failures in user-facing reports or validation gaps.

The report is reset at the start of the parameterized test and written after the test class, including when fingerprint assertions fail. `complete` means all expected APKs initialized and all discovered groups finished; it does not mean every fingerprint passed. An interrupted run can leave an incomplete report, which the helper rejects. Check the timestamps against the run you requested. APK filenames unrecognized by the test factory are excluded from validation.

Helper exit status is `0` when no retained failures or null results remain, `1` when either remains, and `2` for invalid/incomplete reports or unmatched filters. Exit `0` alone does not establish coverage: inspect skips and unobserved records for affected fingerprints. Preserve the Gradle outcome separately to assess build or infrastructure failures.

Use matched member identities for subsequent `dexsql` inspection. Keep patch-specific control-flow checks and comparisons with a patched APK of the same version in the individual task. No HTML, XML, console parsing, or APK-order inference is needed.

After changing the helper, run:

```sh
python -B -m unittest discover -s .agents/skills/merge-upstream-patches/scripts -p 'test_*.py'
```

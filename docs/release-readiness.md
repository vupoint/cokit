# Release Readiness

Use this checklist before publishing a CoKit release candidate or tagging a
public preview. The goal is to prove that the typed API, protocol coverage,
generated-schema provenance, docs, and examples all describe the same surface.

## Required Checks

- Confirm the working tree is on `main`, up to date with `origin/main`, and
  clean before starting release validation.
- Run the full verification gate:

```bash
./gradlew check --stacktrace
```

- Run the real app-server smoke test on a machine with `codex` installed:

```bash
COKIT_CODEX_INTEGRATION=1 ./gradlew :cokit-client:jvmTest --stacktrace
```

- Run the sample CLI help path and verify that it still uses public CoKit APIs:

```bash
./gradlew :cokit-sample-cli:run --args="--help"
```

- Run `git diff --check` and confirm there are no whitespace errors.
- Run `git ls-files`, then run the credential and local-path exposure greps
  listed in `AGENTS.md`. Review every match; expected matches should be limited
  to documented security terms, placeholder protocol fields, or intentional
  loopback test values.
- Re-run the coverage guard tests when protocol descriptors changed, and review
  the generated coverage block in `docs/protocol-compatibility.md`.
- Verify schema provenance in
  `cokit-protocol/src/commonMain/resources/codex-schema-provenance.properties`
  whenever generated schema artifacts or protocol assumptions changed. The
  provenance should record the Codex version, upstream commit or equivalent
  source, generation command, mode, digest or fixture source, and timestamp.
- Confirm README, getting-started docs, and sample CLI examples use typed CoKit
  APIs rather than raw app-server method strings.
- Draft changelog notes covering public API changes, protocol coverage changes,
  experimental API changes, security behavior changes, migration notes, and any
  deferred upstream surfaces.

## Review Gates

- Public docs distinguish supported, experimental, deferred, and compatibility
  surfaces.
- Experimental APIs require explicit Kotlin opt-in and initialization capability
  opt-in where applicable.
- Approval-like server requests remain deny-by-default without application
  handlers.
- Process launch documentation avoids shell interpolation and private machine
  paths.
- No generated artifact is committed without current provenance.
- The release tag or published artifact should not be created until the checks
  above are recorded in release notes or the release issue.

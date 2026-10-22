# Codex 0.163.0 contract fixtures

`release-163-contracts.json` contains synthetic payloads checked against the
0.163.0 stable and experimental schemas generated at release commit
`83086188ae0614bd882818ff1aa23f3194962012`. The `mode` and `schema` fields select
the generated schema; `payload` is the wire value exercised by
`Release163SchemaFixturesTest`. Values use placeholders and do not come from
account or conversation data.

Generation commands and aggregate SHA-256 digests are recorded in
`cokit-protocol/src/commonMain/resources/codex-schema-provenance.properties`.
Normal tests use these fixtures without invoking Codex or installing a validator.
To repeat the independent schema check from the repository root, generate the
pinned schemas and run this with Python's `jsonschema` package available:

```bash
./gradlew :cokit-protocol:generateCodexSchema
python3 - <<'PY'
import json
from pathlib import Path
from jsonschema import Draft7Validator

root = Path("cokit-protocol/build/generated/codex-schema")
fixtures = Path("cokit-client/src/jvmTest/resources/fixtures/release-163-contracts.json")
cases = json.loads(fixtures.read_text())
for case in cases:
    schema = json.loads((root / case["mode"] / "v2" / (case["schema"] + ".json")).read_text())
    Draft7Validator.check_schema(schema)
    Draft7Validator(schema).validate(case["payload"])
print(f"Validated {len(cases)} contract fixtures")
PY
```

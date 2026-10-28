# Codex release contract fixtures

`release-165-contracts.json` contains synthetic payloads checked against the
0.165.0 stable and experimental schemas at release commit
`360319d8dee62d87e2d59fc0c21805d79519cfed`. `Release165SchemaFixturesTest`
round-trips them through public contracts. `release-163-contracts.json` retains
previous regression payloads and is also validated against 0.165.0.
The `mode` and `schema` fields select a schema; `payload` is the wire value.
All values are synthetic placeholders, not account or conversation data.

Generation commands and aggregate SHA-256 digests for the current release are in
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
count = 0
for fixtures in sorted(Path("cokit-client/src/jvmTest/resources/fixtures").glob("release-*-contracts.json")):
    for case in json.loads(fixtures.read_text()):
        schema = json.loads((root / case["mode"] / "v2" / (case["schema"] + ".json")).read_text())
        Draft7Validator.check_schema(schema)
        Draft7Validator(schema).validate(case["payload"])
        count += 1
print(f"Validated {count} contract fixtures")
PY
```

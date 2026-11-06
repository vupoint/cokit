# Codex release contract fixtures

`release-167-contracts.json` contains synthetic payloads checked against the
0.167.0 stable and experimental schemas at release commit
`086ccc833bc9cf01024ad2dd6af7dbd421b6bf9a`. `Release167SchemaFixturesTest`
round-trips them through public contracts. The 0.163 and 0.165 fixtures retain
previous regression payloads and are also validated against 0.167.0.
The `mode` and `schema` fields select a schema; `payload` is the wire value.
All values are synthetic placeholders, not account or conversation data.

An optional `encoded` field records the expected output when defaults are omitted
or upstream schema and wire spelling disagree. The 0.167.0 switch-response schema
uses `login_id` and `auth_url`; the release Rust enum has
`serde(untagged, rename_all_fields = "camelCase")` and the README documents
`loginId` and `authUrl`. CoKit accepts both and encodes camelCase. The raw fixture
is validated against the unmodified schema; the expected encoding follows the
[release serializer](https://github.com/openai/codex/blob/086ccc833bc9cf01024ad2dd6af7dbd421b6bf9a/codex-rs/app-server-protocol/src/protocol/v2/account.rs).
No authenticated browser flow is required by these tests.

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
        bundle = json.loads((root / case["mode"] / "codex_app_server_protocol.schemas.json").read_text())
        schema = {
            "$ref": "#/definitions/v2/" + case["schema"],
            "definitions": bundle["definitions"],
        }
        Draft7Validator.check_schema(schema)
        Draft7Validator(schema).validate(case["payload"])
        count += 1
print(f"Validated {count} contract fixtures")
PY
```

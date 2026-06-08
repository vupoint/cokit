# Protocol Compatibility

CoKit tracks `codex app-server` as an upstream JSON-RPC protocol.

## Versioning

- CoKit library versions follow semantic versioning for public Kotlin APIs.
- Protocol schema metadata records the app-server schema generation command.
- Generated schema outputs are not produced during normal `check`; they are
  generated only when the schema task is explicitly invoked.

## Stable And Experimental APIs

Stable APIs should be available without special opt-in. Experimental APIs should
require explicit Kotlin opt-in annotations and app-server initialization
capabilities where upstream requires them.

The WebSocket transport is currently marked experimental because upstream marks
that transport as experimental.

## Public Client Model Policy

High-level client APIs should accept request objects instead of long parameter
lists. Identifiers and common options should use small value classes such as
`ThreadId`, `TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxPolicy`, and
`ModelName`. Prefer value classes with documented constants over closed enums
when upstream may add new string values.

High-level client models should not expose `JsonElement`, JSON-RPC envelope
types, or other raw protocol payloads directly. Protocol areas that are not yet
modeled should use SDK wrapper types such as `CodexJsonPayload`, named after the
upstream protocol member and kept at the boundary of a typed request or response
model.

Turn input is a public client surface and should use `TurnInput` variants such
as `Text`, `Image`, `LocalImage`, `Skill`, and `Mention` instead of exposing raw
JSON as the primary API. Use `TurnInput.Custom` with `CodexJsonPayload` as an
explicit compatibility escape hatch for upstream variants that CoKit has not
modeled yet.

## Schema Generation

Run:

```bash
./gradlew :cokit-protocol:generateCodexSchema
```

This task runs both stable and experimental schema generation modes:

```bash
codex app-server generate-json-schema --out build/generated/codex-schema/stable
codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental
```

The command requires a local `codex` executable.

## Fixture Policy

Protocol fixtures should come from upstream examples, generated schema samples,
or reduced examples that exercise specific parser behavior. Fixtures must not
include secrets, access tokens, private account data, auth URLs, or private local
paths.

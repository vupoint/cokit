# CoKit Agent Guide

CoKit is planned as a Kotlin Multiplatform client library for `codex app-server`.
It should prioritize a small public API, strict protocol fidelity, and clear module
boundaries over fast one-file implementations.

## Language

- Use English for all documentation, comments, commit messages, public API names,
  test names, sample text, and issue or pull request templates in this repository.
- Avoid adding Korean text to source files, generated artifacts, examples, or docs.

## Project Principles

- Treat the upstream `codex app-server` README and generated schemas as the source
  of truth for protocol behavior.
- Keep stable and experimental protocol surfaces explicit. Experimental APIs must
  require an opt-in at initialization and in CoKit APIs.
- Prefer typed Kotlin APIs backed by `kotlinx.serialization`, while preserving
  unknown JSON payloads for forward compatibility.
- Keep transports separate from protocol and client logic.
- Do not build UI concerns into the library. The library may expose state and
  events that make UI implementation straightforward, but it should not render UI.

## Expected Modules

- `cokit-protocol`: JSON-RPC envelopes, generated protocol DTOs, stable and
  experimental type surfaces, and protocol errors.
- `cokit-rpc`: request correlation, notification dispatch, server-initiated
  request handling, retries, and lifecycle coordination.
- `cokit-client`: high-level thread, turn, auth, model, config, skills, apps,
  filesystem, and process APIs.
- `cokit-transport-stdio`: JVM stdio transport for `codex app-server --stdio`
  and `codex app-server proxy`.
- `cokit-transport-websocket`: opt-in WebSocket transport for experimental and
  local-control scenarios.
- `cokit-testing`: fake transports, transcript fixtures, and protocol test helpers.

## Engineering Workflow

- Start with tests for protocol parsing, request correlation, lifecycle handshake,
  streaming notifications, and approval flows.
- Validate generated protocol code against schema fixtures whenever the upstream
  Codex schema changes.
- Keep public APIs small and documented. Internal APIs may be split aggressively
  when doing so makes request, transport, or protocol behavior easier to reason
  about.
- Do not commit generated schema artifacts without recording the Codex version or
  command that produced them.

## Documentation

- Planning documents live under `docs/`.
- When protocol assumptions change, update both the specification and the
  implementation plan before changing library code.
- Public documentation should distinguish supported, experimental, and deferred
  surfaces.

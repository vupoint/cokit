# Protocol Compatibility

CoKit tracks `codex app-server` as an upstream JSON-RPC protocol.

## Dynamic Tools (Experimental)

CoKit supports `thread/start.dynamicTools` through `StartThreadRequest` and
`ThreadStartParams`. Definitions model the 0.157.1 experimental schema's
`type: "function"` and `type: "namespace"` alternatives; namespaces contain
function definitions only. Function definitions preserve `inputSchema` as
`CodexJsonPayload` and expose optional `deferLoading` (false is omitted).
Names and schemas are subject to upstream validation. CoKit does not orchestrate
tool search for deferred definitions.

Tool construction/handler registration require `ExperimentalCodexApi` opt-in.
Their container types remain usable in stable thread request signatures so
ordinary thread creation does not require experimental opt-in. Non-null tool
lists, including empty lists, require `InitializeCapabilities(experimentalApi =
true)` through both `client.threads.start` and `client.request` overloads.
Null tool lists are omitted, preserving stable thread-start behavior.

`item/tool/call` is decoded into `DynamicToolCallRequest` with `threadId`, `turnId`,
`callId`, `tool`, required opaque `arguments` (including JSON null), and optional
`namespace`. `registerDynamicToolCallHandler` installs a suspending callback;
CoKit replies on the original JSON-RPC id with `contentItems` and `success`.
Results use `inputText`/`text`, `inputImage`/`imageUrl`, and
`inputAudio`/`audioUrl`, distinct from the snake_case turn-tool-output format.
Unknown output variants retain their complete JSON payload.

`item/started` and `item/completed` continue through existing item notifications.
`ItemType.DynamicToolCall` and the optional summary fields `tool`, `namespace`,
`arguments`, `contentItems`, and `success` expose dynamic tool progress/results.
Without a handler, valid calls return `{"contentItems":[],"success":false}`;
this replaces the earlier approval-shaped `decision: "decline"` placeholder.
Malformed params return `-32602`, and handler exceptions return generic `-32000`.
Handlers own permissions and side effects; see [security](security.md#dynamic-tool-execution).

The contract tests use literal payloads checked against the existing generated
0.157.1 experimental schema. Its aggregate SHA-256 is
`ff9bcc67a07f763a9e61954019c652937ae96b1f7ca2319f2ad23d9cb567c571`, matching
`cokit-protocol/src/commonMain/resources/codex-schema-provenance.properties`.
No schema version or generated artifacts are changed by this addition.
Upstream persists tool definitions for resumed threads; the execution callback
remains application-owned and must be registered on each new client connection.
Runtime tool replacement and automatic tool discovery remain deferred.

## Codex 0.157.1 Upgrade

The current baseline is Codex CLI 0.157.1. This release aligns approval kinds,
initialization extensions, MCP form aliases, section and history APIs, effective
thread/turn metadata, attachments, Gateway OAuth and experimental project, queue
and native verification APIs. Public contracts remain in `cokit-client-api`;
transport and caller-owned UI/authentication policy remain separate.

## Versioning

- CoKit library versions follow semantic versioning for public Kotlin APIs.
- Protocol schema metadata records the app-server schema generation command.
- Generated schema outputs are not produced during normal `check`; they are
  generated only when the schema task is explicitly invoked.

## Stable And Experimental APIs

Stable APIs should be available without special opt-in. Experimental APIs should
require explicit Kotlin opt-in annotations and app-server initialization
capabilities where upstream requires them.

Client-level experimental protocol surfaces use `ExperimentalCodexApi`.
Experimental descriptors must require `@ExperimentalCodexApi` at the Kotlin API
usage site and must also require the matching app-server initialization
capability opt-in before they become usable.

The WebSocket transport is currently marked experimental because upstream marks
that transport as experimental.

Standalone process lifecycle descriptors are also experimental. `CodexRpc.Process`
and its params models require `@ExperimentalCodexApi`, and applications should
enable the matching upstream experimental capability before use.

Remote-control descriptors are experimental. `CodexRpc.RemoteControl` and its
status models require `@ExperimentalCodexApi`, and applications should enable
the matching upstream experimental capability before use. Current local
`codex-cli 0.157.1` generated schema exposes the enable/disable params and
status-changed notification shape; the upstream README also documents the
`remoteControl/enable`, `remoteControl/disable`, and
`remoteControl/status/read` request methods.

## Public Client Model Policy

The primary client API is JSON-RPC-first. It should expose upstream method names
through typed descriptors such as `CodexRpc.Thread.Start`, not through ad hoc
raw strings. Each descriptor should bind one typed params model to one typed
result model.

All modeled thread and turn request methods should be present in the
`CodexRpc` descriptor catalog. Compatibility facades such as thread and turn
helpers should delegate through those descriptors instead of carrying separate
method strings.

Client APIs should accept request objects instead of long parameter lists.
Identifiers and common options should use focused types such as `ThreadId`,
`TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxMode`, `SandboxPolicy`, and
`ModelName`. `thread/start.sandbox` is a kebab-case `SandboxMode` string;
`turn/start.sandboxPolicy` and `command/exec.sandboxPolicy` are structured
`SandboxPolicy` objects. `ApprovalPolicy` accepts the current stable string
values and the granular stable object, while the legacy `on-failure` value is
decode-compatible but deprecated. Stable thread start, resume, fork, and list
requests expose the reviewed stable field set. Thread list cwd filters preserve
the upstream string-or-array union, list results expose both forward and
backward cursors, `thread/unarchive` returns its refreshed thread, and
`turn/steer` returns the accepted turn id. Thread, turn, item, pagination, and
client-message scalar fields should likewise use wrappers such as `CodexCursor`,
`CodexTimestamp`, `ClientMessageId`, `ThreadStatusType`, `TurnStatus`, `ItemId`,
and `ItemStatus`.

CoKit is still in the `0.0.x` development series, so inaccurate public contracts
are corrected rather than retained as stable-looking aliases. For this upgrade:

- Replace `isPinned` with `sectionId`: `CodexOptional.Omitted` leaves the filter
  unset, `CodexOptional.Value(null)` selects unsectioned threads and a non-null
  value selects a section. `ThreadSectionUpdateParams.appearance` uses the same
  omission/clear/value distinction.
- `excludeTurns` on resume/fork and `thread/turns/list` are stable in 0.157.1.
  Preserve history mode and pagination cursors rather than assuming all turns
  were loaded. `initialTurnsPage` remains outside the stable request API.
- Thread status notifications carry structured status, including active flags.
- `thread/rollback` is removed upstream; `thread/revert` only changes conversation
  history and does not undo filesystem changes.
- Do not rely on friendly/pragmatic personality constants to select a style;
  both are deprecated. The `none` value retains instruction-removal semantics.

Prefer value classes with documented constants over closed enums when upstream
may add new string values.

Primary client models should not expose `JsonElement`, JSON-RPC envelope types,
or other raw protocol payloads directly. Protocol areas that are not yet modeled
should be deferred from the primary API or kept behind explicit compatibility
types such as `CodexJsonPayload`; examples and getting-started documentation
should not require consumers to construct arbitrary JSON.

Turn input is a public client surface and should use `TurnInput` variants such
as `Text`, `Image`, `LocalImage`, `Skill`, and `Mention` instead of exposing raw
JSON as the primary API. Use `TurnInput.Custom` with `CodexJsonPayload` as an
explicit compatibility escape hatch for upstream variants that CoKit has not
modeled yet.

Notifications and server-initiated requests should be modeled as typed sealed
interfaces. Unknown notifications may expose the upstream method name, but they
should not expose raw JSON in the primary API. Approval-like server requests must
remain deny-by-default unless a typed handler is registered.

## Upstream Coverage Snapshot

This snapshot was reviewed against the upstream app-server README and generated
`codex-cli 0.157.1` stable and experimental schemas on 2026-09-27:

https://github.com/openai/codex/blob/rust-v0.157.1/codex-rs/app-server/README.md

The checked protocol inventory groups upstream request, notification, and
server-request surfaces by current CoKit coverage:
[Protocol Inventory](protocol-inventory.md).

The exact descriptor catalog is maintained once in the checked inventory above.

`CodexClients.connect()` also performs the required `initialize` request and
`initialized` notification internally.

The following summary is checked by `CodexRpcCoverageTest`. Inventory group
counts are derived from `docs/protocol-inventory.md`; the public `CodexRpc`
request descriptor count is exact.

<!-- codex-rpc-coverage:start -->
| Inventory section | `modeled` | `partial` | `deferred` | `experimental` | Exact current coverage |
| --- | ---: | ---: | ---: | ---: | --- |
| Request groups | 9 | 8 | 5 | 12 | 114 public `CodexRpc` request descriptors |
| Notification groups | 6 | 5 | 7 | 8 | Not counted by this helper |
| Server-request groups | 0 | 5 | 0 | 2 | Not counted by this helper |
<!-- codex-rpc-coverage:end -->

The pinned stable schema contains 104 client requests; CoKit exposes 83 of them
as public descriptors and implements `initialize` internally (84/104, 80.8%).
The experimental bundle contains 167 client requests, including stable methods;
CoKit exposes 114 descriptors in total and implements `initialize` internally
(115/167, 68.9%). These are method counts, not claims of complete field or event
coverage. No current descriptor is absent from the pinned experimental bundle.

Typed notification and server-request coverage is intentionally smaller than the
upstream surface today:

- Notifications: `CodexNotification.ThreadStarted`, `ThreadStatusChanged`,
  `ThreadTokenUsageUpdated`, `TurnStarted`, `TurnCompleted`, `TurnFailed`,
  `ItemStarted`, `ItemCompleted`, `AgentMessageDelta`,
  `ReasoningSummaryTextDelta`, `Warning`, `ConfigWarning`, `Error`,
  `ServerRequestResolved`, and `CommandExecOutputDelta` are modeled.
  `TurnFailed` is decoded from upstream `turn/completed` notifications with
  `turn.status == "failed"`. Item lifecycle events expose a `ThreadItemSummary`
  compatibility wrapper for common rendering fields.
  Error notifications expose safe message fields; structured `codexErrorInfo`
  remains deferred until it has a typed compatibility model.
  `serverRequest/resolved` currently carries only `threadId` and `requestId`;
  CoKit preserves that schema shape instead of fabricating method or status
  fields. `command/exec/outputDelta` currently carries base64 stdout/stderr
  chunks and `capReached`; command exit status remains part of the final
  `command/exec` response, and command failures use JSON-RPC errors rather than
  separate notifications. `fs/changed` carries a connection-scoped watch id and
  changed host paths for `fs/watch` subscribers; current upstream does not
  include a separate event-kind field. `AccountLoginCompleted` reports login
  success or failure, preserving nullable login ids for API-key and canceled
  flows. `AccountRateLimitsUpdated` carries sparse rolling rate-limit snapshots
  that clients can merge into the latest read response.
  Attachment and Gateway OAuth mutations, plus experimental project and queue
  changes, also have typed events.
  `RemoteControlStatusChanged` exposes the experimental remote-control status
  snapshot, including local server name and client-visible environment id.
  Unknown notifications expose only the method name in the primary API.
- Server requests: command execution approval, file-change approval, permission
  approval, tool user-input prompts, and MCP elicitations are modeled with typed
  handlers. Permission approvals return granted permission subsets instead of
  command-style decision strings. User-input prompts return typed answer maps
  only from explicit handlers and cancel by default. MCP elicitations expose
  form and URL requests and decline by default. Approval-like request families
  without typed handlers remain deny-by-default.

Review start is modeled as protocol data only. `CodexRpc.Review.Start` accepts
typed review targets and returns the thread and turn where the review runs, but
CoKit does not render review UI or make product decisions about presenting
findings.

Model catalog APIs are modeled as read-only protocol data. `CodexRpc.Model.List`
returns typed catalog entries with display names, selected provider model names,
reasoning options, input modalities, service tiers, and pagination cursors.
`CodexRpc.Model.ReadProviderCapabilities` exposes provider feature flags for web
search, image generation, and namespace tools without adding UI policy.

Config read and write APIs are partially modeled. `CodexRpc.Config.Read` returns
the effective app-server config plus optional layer metadata, while keeping
arbitrary config values behind `ConfigValue` and `CodexJsonPayload`.
`CodexRpc.Config.WriteValue` and `BatchWrite` expose key-path edits, merge
strategy, expected-version checks, explicit file paths, and the batch
`reloadUserConfig` flag. `CodexRpc.Config.ReadRequirements` exposes read-only
managed policy constraints for approval policies, sandbox modes, permission
profiles, web-search modes, Windows sandbox setup modes, remote control,
feature pins, residency, computer-use, login/provider restrictions, browser and
credential-store requirements. The old network field is deprecated compatibility only. Managed
hook requirement details remain compatibility-limited.

Permission profile and environment catalog APIs are modeled according to the
current generated schema. `CodexRpc.PermissionProfile.List` reads server-defined
permission profile ids and descriptions for an optional host cwd.
`CodexRpc.CollaborationMode.List` and `CodexRpc.Environment.Add` are
experimental and require `ExperimentalCodexApi`; current `codex-cli 0.157.1`
schema defines collaboration mode listing and environment registration, but not
environment list/read or collaboration mode read descriptors.

Skills and hooks APIs are modeled as data-oriented protocol descriptors.
`CodexRpc.Skills.List` returns per-cwd skill metadata, parse errors, dependency
declarations, and optional interface metadata without loading or executing skill
content in CoKit. `CodexRpc.Skills.SetExtraRoots` updates the app-server skill
search roots, and `CodexRpc.Skills.WriteConfig` changes a skill's enabled state
by name or path. Current `codex-cli 0.157.1` schema defines
`skills/config/write` but not a `skills/config/read` request.
`CodexRpc.Hooks.List` returns per-cwd hook metadata, warnings, and parse errors
without executing hook handlers in CoKit.

App catalog APIs are partially modeled. `CodexRpc.App.Installed` exposes the
stable committed runtime snapshot without importing experimental connector
metadata. `CodexRpc.Apps.List` exposes the experimental `app/list` page shape
behind `ExperimentalCodexApi`, including branding, labels,
enabled/accessibility flags, and optional app metadata. CoKit does not render
app UI, authenticate apps, install plugins, or invoke app behavior through these
descriptors.

Plugin and marketplace APIs are partially modeled as catalog and installation
descriptors. `CodexRpc.Plugin.List`, `Installed`, `Read`, and `ReadSkill` expose
marketplace entries, plugin summaries, local/git/remote source metadata, skill
content reads, app summaries, hook summaries, and MCP server names as protocol
data. `CodexRpc.Plugin.Install` returns app-auth requirements and
`CodexRpc.Plugin.Uninstall` models the current empty response shape.
`CodexRpc.Marketplace.Add`, `Remove`, and `Upgrade` expose marketplace source
and filesystem-root metadata while leaving policy and UI decisions to the
calling application. Current plugin sharing methods remain deferred.

MCP request APIs are modeled as data descriptors. `CodexRpc.Mcp` exposes OAuth
login, config reload, server status listing, resource reads, and tool calls.
MCP-provided resource metadata, tool schemas, annotations, tool arguments,
content arrays, structured content, and `_meta` payloads remain opaque
`CodexJsonPayload` values so clients can preserve wire compatibility without
CoKit interpreting connector-specific data or rendering connector UI.

Account request APIs are modeled. `CodexRpc.Account.Read` exposes the current
account state, whether OpenAI auth is required, API-key and Amazon Bedrock
account markers, and ChatGPT plan plus redacted email data.
`CodexRpc.Account.StartLogin` models API-key, ChatGPT browser, ChatGPT
device-code, and unstable external-token login starts. Auth URLs, user codes,
API keys, access tokens, and account identifiers are redacted from model string
representations. `CodexRpc.Account.CancelLogin` models the current cancel-status
response, and `CodexRpc.Account.Logout` models the current no-params empty
response. `CodexRpc.Account.ReadRateLimits`, `ReadUsage`, and
`SendAddCreditsNudgeEmail` model rate-limit snapshots, usage summaries, daily
usage buckets, and add-credits nudge email status.
`CodexRpc.Account.ReadWorkspaceMessages` exposes stable workspace messages, and
`ConsumeRateLimitResetCredit` requires an explicit idempotency key and returns
one of the four stable reset-credit outcomes.

Remote-control APIs are modeled behind experimental opt-in.
`CodexRpc.RemoteControl.Enable`, `Disable`, and `ReadStatus` return the current
status snapshot with `status`, `installationId`, `serverName`, and nullable
`environmentId`. `StartPairing` and `ReadPairingStatus` model pairing codes,
manual pairing codes, expiration time, and claimed status. `ListClients` and
`RevokeClient` model controller-device metadata and revocation by environment
and client id. Pairing codes, manual pairing codes, and client ids are redacted
from model string representations. CoKit treats these values as protocol data
only and does not decide whether remote access should be enabled, paired, or
revoked.

The following upstream request groups are not yet modeled as primary typed
descriptors:

- Advanced thread APIs: settings, memory mode, shell commands, background
  terminals, realtime, search/timeline and raw item injection.
- Catalog and configuration APIs: feature catalog/enablement, app read, Windows
  sandbox, feedback upload, diagnostics, and external-agent migration.
- Plugin sharing APIs: share save, update targets, list, checkout, and delete.
Future work should add these groups as typed descriptor namespaces without
changing the rule that primary APIs do not expose `JsonElement`, raw method
strings, or JSON-RPC envelopes.

Newly supported stable and experimental release extensions are documented below.
Item-anchor cursors currently exist only on upstream main; CoKit retains the
released string-cursor contract. Deferred features are listed in the inventory,
including released experimental diagnostics and plugin search.

## Implementation Roadmap

This roadmap orders the remaining protocol work by SDK usefulness, protocol
risk, and security sensitivity. It is not a release commitment; it is the
preferred implementation sequence when extending CoKit toward the full upstream
app-server surface.

### Phase 1: Protocol Inventory And Generation

Goal: make the upstream README and generated schema the repeatable source of
truth before broadening the public API.

- Maintain a checked protocol inventory that groups every upstream method into
  stable, experimental, notification, and server-request surfaces.
- Generate stable and experimental schema fixtures from a recorded Codex version
  or upstream commit, and validate generated DTOs against those fixtures.
- Add a public coverage table that reports modeled request descriptors, typed
  notifications, typed server requests, and explicit compatibility gaps.
- Keep `CodexJsonPayload` limited to documented compatibility fields and avoid
  adding new primary APIs that require callers to construct arbitrary JSON.

Exit criteria:

- Coverage can be recalculated from a documented upstream README/schema version.
- New method descriptors cannot be added without params/result serializer tests.
- Stable and experimental surfaces are visibly separated in source and docs.

### Phase 2: Core Thread And Turn Fidelity

Goal: complete the ordinary conversation lifecycle before utility and account
surfaces.

- Expand typed models for thread metadata, status, settings, permission profile
  selection, runtime workspace roots, environments, token usage, and paged turn
  history.
- Add descriptors for loaded-thread listing, turn history paging, metadata
  updates, settings updates, memory mode, goals, delete, compaction, shell
  command, background terminals, and realtime methods.
- Model the event stream needed by normal clients: `thread/*`, `turn/*`,
  `item/started`, `item/completed`, item deltas, token usage, warnings, and
  errors.
- Keep unsupported upstream methods, such as currently unsupported item
  hydration endpoints, out of the stable convenience surface until CoKit can
  represent their unsupported state intentionally.

Exit criteria:

- A client can start, resume, fork, page, render, steer, interrupt, archive,
  restore, delete, and observe a thread without raw JSON.
- Event rendering code can rely on typed sealed models for common thread, turn,
  item, warning, and error notifications.
- Experimental thread and realtime APIs require explicit initialization and API
  opt-in.

### Phase 3: Server-Initiated Requests And Approvals

Goal: make app-server initiated work safe, typed, and deny-by-default.

- Add typed server-request models and handlers for command approval, file-change
  approval, permission requests, dynamic tool calls, MCP elicitation, user input
  requests, and attestation generation.
- Preserve automatic decline or cancel defaults for approval-like requests when
  no handler is registered.
- Permission request handlers grant only explicit filesystem and network
  subsets; an empty grant is the default denial shape and session grants must be
  requested with `scope: "session"`.
- Attestation generation handlers require explicit application registration and
  return opaque client-owned tokens; the absence of a handler returns
  unsupported by default.
- Emit typed request lifecycle notifications such as `serverRequest/resolved`
  so applications can clear pending UI state reliably.
- Document handler trust boundaries, host path semantics, and persistence
  behavior for session-scoped grants.

Exit criteria:

- Applications can choose safe defaults or explicit handlers without seeing
  JSON-RPC envelopes.
- Security tests cover malformed requests, missing responses, default denial,
  and handler failures.
- No handler accepts command execution, file changes, permission grants, tool
  calls, elicitation, attestation, or user input by default.

### Phase 4: Host Utilities And Execution APIs

Goal: expose host-side utility APIs after the approval and event model is strong
enough to observe side effects.

- Add typed descriptors and models for `command/exec`, streaming command input
  and output, command resizing, and command termination.
- Add typed descriptors and models for filesystem read/write, directory,
  metadata, copy, remove, watch, and unwatch methods.
- Add experimental typed descriptors for standalone process lifecycle APIs.
- Add review-start support once review items and detached review threads are
  modeled well enough for consumers to render results safely.

Exit criteria:

- Command and process APIs document sandbox differences, host semantics, output
  caps, timeout behavior, and connection-scoped lifecycle.
- Filesystem APIs require explicit absolute host paths in typed value models.
- Streaming output and filesystem watch notifications are typed and bounded.

### Phase 5: Catalog, Configuration, And Extension Surfaces

Goal: let applications inspect app-server capabilities and extension metadata
without implementing local UI policy inside CoKit.

- Add model catalog, model-provider capability, experimental feature,
  permission-profile, environment, collaboration-mode, and config read/write
  descriptors.
- Add skills, skill config, extra skill roots, hooks, apps, marketplace, plugin,
  and plugin-skill descriptors.
- Add MCP status, resource read, tool call, OAuth login, and config reload
  descriptors.
- Keep plugin, app, hook, and MCP APIs data-oriented. CoKit should expose typed
  state and events, not render UI or decide user policy.

Exit criteria:

- Clients can build settings, catalog, extension, and MCP screens from typed
  models.
- Experimental and under-development plugin/app APIs remain opt-in and clearly
  marked.
- Documentation distinguishes data surfaces from UI responsibilities.

### Phase 6: Auth, Account, Remote Control, And Managed Policy

Goal: complete account and remote-control support after core local protocol
surfaces are reliable.

- Add account read, login start, login cancel, logout, usage, rate limit, and
  add-credits notification descriptors and models.
- Add account update, login completed, rate-limit update, and MCP OAuth
  completion notifications.
- Add remote-control enable, disable, status, pairing, client list, client
  revoke, and status-change models behind experimental opt-in.
- Add config requirements and managed policy models so applications can explain
  disabled capabilities without duplicating raw config parsing.

Exit criteria:

- Auth flows avoid logging API keys, auth URLs, tokens, emails, and account
  identifiers by default.
- Remote-control APIs document enrollment, pairing, revocation, and local trust
  boundaries before becoming usable.
- Managed policy constraints can be surfaced through typed read-only models.

### Phase 7: Compatibility, Hardening, And Release Readiness

Goal: make the API safe to publish and maintain as upstream evolves.

- `checkPublicApiExposure` runs during `check` and prevents accidental primary
  client API exposure of `JsonElement` and JSON-RPC envelope types. Use typed
  models or `CodexJsonPayload` for documented compatibility fields.
- `checkKotlinAbi` runs during `check` for published Kotlin library modules and
  compares compiled ABI declarations against the committed KGP reference dumps
  under each module's `abi/` directory. Review intentional API changes, then
  refresh the reference dumps with `./gradlew updateKotlinAbi`.
- `checkPrimaryApiDocsAlignment` keeps README, getting-started, and the sample
  CLI centered on typed CoKit APIs. Raw app-server method strings belong in
  protocol compatibility or inventory docs, not primary examples.
- Add high-rate notification, malformed message, oversized message, overload,
  retry, and shutdown tests across protocol, RPC, transport, and client modules.
- Reject malformed JSON-RPC envelopes during protocol decode and enforce an
  encoded-size limit in `JsonRpcSession` before messages enter request,
  notification, or transport buffers.
- Surface app-server overload errors as retryable remote exceptions, but keep
  retries application-owned and bounded rather than automatic.
- Make RPC and stdio shutdown idempotent, cancel pending requests on session
  close, destroy stdio app-server processes once, and drain stderr when present.
- Keep sample CLI and getting-started docs aligned with the primary typed API.
- Re-run public exposure and security scans before release candidates.

Exit criteria:

- `./gradlew check --stacktrace` and public exposure/security grep pass.
- Docs include supported, experimental, deferred, and compatibility surfaces.
- Release candidates follow the [Release Readiness](release-readiness.md)
  checklist before tagging or publishing.
- A new upstream README/schema update has a documented workflow for updating
  descriptors, DTOs, tests, sample code, and coverage tables together.

## Schema Generation

Schema provenance is recorded in
`cokit-protocol/src/commonMain/resources/codex-schema-provenance.properties`.
The file records the Codex CLI version, upstream Codex commit, stable schema
command, experimental schema command, generation timestamp, and canonical
SHA-256 digests for both schema modes. The current stable baseline is
`codex-cli 0.157.1` at upstream release commit
`36650394c5b38c2990ccf2a3457165ca3e9d9726`.

Run:

```bash
./gradlew :cokit-protocol:generateCodexSchema
```

This task runs both stable and experimental schema generation modes:

```bash
codex app-server generate-json-schema --out build/generated/codex-schema/stable
codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental
```

The command requires a local `codex` executable. Before refreshing generated
schema fixtures or generated DTOs, update the provenance file with:

- the exact `codex --version` output;
- the release commit associated with that CLI build;
- the generation timestamp and exact commands;
- SHA-256 digests computed from compact JSON with recursively sorted object
  keys (`jq -S -c .`) so generated definition ordering does not change the
  recorded digest.

```bash
codex --version
git ls-remote https://github.com/openai/codex.git 'refs/tags/rust-v0.157.1^{}'
```

Then update `generatedAt` to the refresh timestamp. The Gradle schema generation
tasks validate that the provenance file exists, includes every required key, and
records the stable or experimental command for the generation mode being run.
They also reject generation when the installed `codex --version` output differs
from the recorded version.
Do not commit generated schema outputs unless the provenance file is updated in
the same change.

## Fixture Policy

Protocol fixtures should come from upstream examples, generated schema samples,
or reduced examples that exercise specific parser behavior. Fixtures must not
include secrets, access tokens, private account data, auth URLs, or private local
paths.

## Codex 0.157.1 Model Alignment

Thread start/resume/fork results retain effective configuration and resume history
cursors. Thread snapshots retain metadata, runtime status, and included turns.
Turn results retain timestamps and duration. `serviceTierForTurn` changes only a new
turn; `serviceTier` continues to change subsequent turns. `toolOutput` accepts text
or typed multimodal content. `turnTrigger` and `disabledPluginIds` are forwarded;
upstream currently saves disabled plugin IDs without filtering capabilities.
`Personality.Friendly` and `Pragmatic` are deprecated because they no longer select
a style. `none` retains the upstream instruction-removal semantics.

Permission profile availability is nullable for older servers, never inferred as
allowed. MCP snapshots preserve connection status, capabilities, plugin origin and
catalog errors. Resource reads support explicit app/account targets and origin call
IDs. Model discovery preserves specialty, multi-agent version, access programs and
retirement timestamps. Managed policy exposes stable login, provider, browser,
auto-review and credential-store requirements. Legacy `network` is decode-only
compatibility with older servers, not a current stable requirement field.

## Attachments And Gateway OAuth

`CodexRpc.ThreadAttachment` adds, lists and removes stored resource references;
`ThreadAttachmentUpdated` reports explicit mutations. Fork attachment copying is
best effort and does not emit per-attachment events; reload the new thread's list.
The server limits attachments to 100 per thread/page. No external resource is
created, copied or deleted by CoKit.

`CodexRpc.GatewayOAuth` exposes read/login/cancel and `GatewayOAuthChanged` exposes
status and the initiating connection's authorization URL. Set
`InitializeCapabilities.explicitGatewayOauth = true` and successfully call Read on
every connection before authenticated calls. Failed or unsupported probes require
resolution or a server upgrade; do not fall back to automatic browser login.
Login starts only on an explicit call, and clients own opening the returned event
URL. Do not opt out of gateway notifications while logging in. Cancel applies to
the calling connection and acknowledges release of its login slot.

## Experimental Release Extensions

`CodexRpc.Project` exposes list/read/create/import/update/move/delete.
`CodexRpc.ThreadQueue` exposes add/list/update/delete/reorder/start. Queue input
reuses `TurnInput`, including its opaque forward-compatible variant. Project,
thread-project and queue change notifications are typed and experimental.

`CodexRpc.UserVerification` exposes status/enroll/delete/verify/cancel. All three
new request groups require `@OptIn(ExperimentalCodexApi::class)` and
`InitializeCapabilities(experimentalApi = true)`. Missing initialization opt-in
fails locally before sending. Native verification errors remain ordinary
`JsonRpcRemoteException` errors with upstream error data; CoKit does not turn
verification into an automatic approval handler.

The `CodexClient.request(method, params, onRequestId)` overload reports an outbound
`CodexRequestId` after sending. Keep this ID when an upstream cancellation request
needs it. Coroutine cancellation only releases local correlation state. See the
native verification trust and cancellation requirements in `security.md`.

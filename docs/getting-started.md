# Getting Started

## Install

During local development, depend on project modules:

```kotlin
dependencies {
    implementation(project(":cokit-client"))
    implementation(project(":cokit-transport-stdio"))
}
```

Published artifacts use the `io.github.vupoint.cokit` group. Import the CoKit
BOM to align module and JVM target artifact versions, then declare the CoKit
modules you need without repeating the version. The sample CLI is a repository
example and is not published as a library artifact.

```kotlin
dependencies {
    implementation(platform("io.github.vupoint.cokit:cokit-bom:<version>"))
    implementation("io.github.vupoint.cokit:cokit-client")
    implementation("io.github.vupoint.cokit:cokit-transport-stdio")
}
```

Use `cokit-client-api` directly only for API contracts and models without the
default `CodexClients` factory.

## Run The Sample CLI

The repository includes a small JVM CLI sample in `:cokit-sample-cli`.

```bash
./gradlew :cokit-sample-cli:run --args="--help"
```

When `codex` is installed locally, the sample can start a real app-server
thread, send the default message, and stream the assistant response over stdio
without requiring arguments:

```bash
./gradlew :cokit-sample-cli:run
```

Optional `--cwd` and `--message` flags can override the defaults:

```bash
./gradlew :cokit-sample-cli:run --args='--cwd /path/to/project --message "Summarize this repository"'
```

The sample uses CoKit's default stdio app-server transport.

The command prints the created thread and turn ids before streaming assistant
text from typed `CodexNotification.AgentMessageDelta` events. A completed turn
with no assistant text is reported explicitly, and failed turns exit with the
app-server error message.

## Connect To app-server

```kotlin
val transport = StdioCodexTransport()

val client = CodexClients.connect(
    CodexClientConnection(
        transport = transport,
        clientInfo = ClientInfo(
            name = "my_kotlin_client",
            title = "My Kotlin Client",
            version = "0.1.0",
        ),
        scope = scope,
    ),
)
```

`connect()` sends `initialize`, waits for the matching response, then sends the
`initialized` notification.

## Send JSON-RPC Requests

```kotlin
val thread = client.request(
    CodexRpc.Thread.Start,
    ThreadStartParams(
        cwd = CodexHostPath("/path/to/project"),
        approvalPolicy = ApprovalPolicy.OnRequest,
        sandbox = SandboxMode.WorkspaceWrite,
    ),
).thread

val turn = client.request(
    CodexRpc.Turn.Start,
    TurnStartParams(
        threadId = thread.id,
        input = listOf(TurnInput.Text("Summarize this repository")),
    ),
).turn
```

`CodexRpc` descriptors expose upstream operations through typed names such as
`CodexRpc.Thread.Start` and `CodexRpc.Turn.Start` without asking callers to pass
raw strings or JSON payloads. Each descriptor binds exactly one params type to
one result type, so `CodexRpc.Thread.Start` accepts `ThreadStartParams` and
returns `ThreadStartResult`.

Identifiers and common options use lightweight SDK value types such as
`ThreadId`, `TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxMode`,
`SandboxPolicy`, `ModelName`, and `TurnInput` so application code is explicit
while preserving the upstream wire shape. `StartThreadRequest.sandbox` uses the
kebab-case `SandboxMode` string, while turn and command overrides use the
structured `SandboxPolicy` object.

## Inspect The Model Catalog

```kotlin
val page = client.request(
    CodexRpc.Model.List,
    ModelListParams(includeHidden = false),
)

val capabilities = client.request(
    CodexRpc.Model.ReadProviderCapabilities,
    ModelProviderCapabilitiesReadParams,
)

page.data.forEach { model ->
    println("${model.displayName}: ${model.model.value}")
}

if (capabilities.webSearch) {
    println("The configured provider supports web search.")
}
```

Model catalog descriptors are read-only protocol surfaces. `CodexRpc.Model.List`
returns typed catalog entries, pagination cursors, reasoning-effort options, and
service-tier metadata. `CodexRpc.Model.ReadProviderCapabilities` reports
provider-level feature flags such as web search, image generation, and namespace
tools without requiring callers to build raw JSON-RPC requests.

## Inspect Stable Runtime State

```kotlin
val loadedThreads = client.threads.listLoaded(ListLoadedThreadsRequest(limit = 20))

val installedApps = client.request(
    CodexRpc.App.Installed,
    AppsInstalledParams(forceRefresh = false),
)

val workspaceMessages = client.request(
    CodexRpc.Account.ReadWorkspaceMessages,
    AccountWorkspaceMessagesReadParams,
)
```

These stable, read-only calls expose the app-server's loaded thread ids,
installed app snapshot, and workspace messages without importing experimental
connector metadata. Reset-credit consumption is intentionally omitted from the
read-only quickstart: callers must supply an explicit idempotency key and handle
all documented outcomes before invoking that mutation.

## Sections And Paginated History

The 0.160.0 baseline uses sections in place of the old `isPinned` filter.
Omitting `sectionId` leaves the filter unset; explicit null selects threads that
are not in any section:

```kotlin
val unsectioned = client.request(
    CodexRpc.Thread.List,
    ThreadListParams(sectionId = CodexOptional.Value(null), limit = 20),
)
val sections = client.request(CodexRpc.ThreadSection.List, ThreadSectionListParams())
val page = client.request(
    CodexRpc.Thread.ListItems,
    ThreadItemsListParams(threadId = threadId, limit = 50),
)
```

Use returned cursors to request later pages. Item history preserves payloads in
`CodexJsonPayload`, since not every upstream item variant is modeled. Resume and
fork support stable `excludeTurns`; inspect the returned history mode rather than
assuming an empty list means the thread has no history.

Project, queue and native verification descriptors require both Kotlin opt-in
and `InitializeCapabilities(experimentalApi = true)` when connecting. These are
explicit operations; merely connecting does not queue input, start a turn, enroll
a credential or request a biometric proof. See [Protocol Compatibility](protocol-compatibility.md)
and [Security](security.md) for Gateway OAuth probing and native cancellation.

## Observe Notifications

```kotlin
client.notifications.collect { notification ->
    when (notification) {
        is CodexNotification.ThreadStarted -> println(notification.threadId.value)
        is CodexNotification.ThreadStatusChanged -> println(notification.status.value)
        is CodexNotification.ThreadTokenUsageUpdated -> println(notification.tokenUsage.total.totalTokens)
        is CodexNotification.TurnStarted -> println(notification.turn.id.value)
        is CodexNotification.TurnCompleted -> println(notification.turn.status.value)
        is CodexNotification.TurnFailed -> println(notification.turn.error?.message)
        is CodexNotification.ItemStarted -> println(notification.item.type.value)
        is CodexNotification.ItemCompleted -> println(notification.item.status?.value)
        is CodexNotification.AgentMessageDelta -> print(notification.delta)
        is CodexNotification.ReasoningSummaryTextDelta -> print(notification.delta)
        is CodexNotification.Warning -> println(notification.message)
        is CodexNotification.ConfigWarning -> println(notification.summary)
        is CodexNotification.Error -> println(notification.error.message)
        is CodexNotification.ServerRequestResolved -> println(notification.requestId)
        is CodexNotification.FilesystemChanged -> println(notification.changedPaths.size)
        is CodexNotification.Unknown -> println(notification.method)
    }
}
```

`notifications` exposes typed CoKit notification models. Unknown notifications
keep the method name but do not expose raw JSON through the primary API.
Turn failures are decoded from typed turn-completion notifications whose turn
status is `failed`. Item lifecycle notifications expose `ThreadItemSummary` for
common rendering fields, and streamed text deltas are typed for agent messages
and reasoning summaries. Token usage, warning, config warning, and error
notifications are typed without exposing raw notification params.
`ServerRequestResolved` exposes the thread id and JSON-RPC request id so
applications can clear pending approval, elicitation, attestation, or user-input
UI when app-server reports that the request was answered or cleared.
`FilesystemChanged` exposes the connection-scoped filesystem watch id and the
changed host paths reported by app-server.

## Handle Server Requests

CoKit defaults approval-like requests to safe responses when no handler is
registered. Consumers can register handlers for methods that need custom
decisions:

```kotlin
client.registerCommandApprovalHandler { request ->
    println(request.command)
    ApprovalDecision.Decline
}
```

Command approval handlers use typed request and decision models. Raw JSON-RPC
compatibility hooks remain available for advanced protocol work, but they are
not the default application API.

## Codex 0.160.0 Item History

Request a page after a known item within a visible turn. Ascending order returns
newer items; use `SortDirection.Desc` to read older items instead.

```kotlin
val page = client.request(
    CodexRpc.Thread.ListItems,
    ThreadItemsListParams(
        threadId = ThreadId("thread_123"),
        turnId = TurnId("turn_123"),
        cursor = ThreadItemsListCursor.ItemAnchor(ItemId("item_123")),
        sortDirection = SortDirection.Asc,
    ),
)
val continuation = page.nextCursor?.let { ThreadItemsListCursor.Opaque(it) }
```

Only send a continuation request when `nextCursor` is non-null. Passing null again
starts the first page. Existing string request cursors now need the `Opaque`
wrapper; response cursors and other pagination APIs keep `CodexCursor`.

For focused MCP discovery, pass `serverName = McpServerName("example")` in
`McpServerStatusListParams`, with `threadId` when querying a loaded thread's
connection. MCP OAuth can pass that thread and an explicit
`McpServerOauthClientRegistration` when required by the configured server.

For item notifications, `item.mcpAppUi` contains an explicit display preference;
`item.mcpAppResourceUri` may still exist when the preference is absent. Applications
own display decisions and resource loading. CoKit supplies metadata only.

Treat only `TurnStatus.Completed` as successful completion. A `TurnCompleted`
notification can carry `TurnStatus.Interrupted` and an error such as
`tooManyDenials`; there may be no separate `CodexNotification.Error` event.

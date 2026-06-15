# Getting Started

## Install

CoKit is not published yet. During local development, depend on project modules:

```kotlin
dependencies {
    implementation(project(":cokit-client"))
    implementation(project(":cokit-transport-stdio"))
}
```

Future published artifacts are expected to use the `io.github.cokit` group.

## Run The Sample CLI

The repository includes a small JVM CLI sample in `:cokit-sample-cli`.

```bash
./gradlew :cokit-sample-cli:run --args="--help"
```

When `codex` is installed locally, the sample can start a real app-server thread
and turn over stdio without requiring arguments:

```bash
./gradlew :cokit-sample-cli:run
```

Optional `--cwd` and `--message` flags can override the defaults:

```bash
./gradlew :cokit-sample-cli:run --args='--cwd /path/to/project --message "Summarize this repository"'
```

The sample uses CoKit's default stdio app-server transport. Use
`COKIT_CODEX_COMMAND` only when you need to point it at another local command
during development.

## Connect To app-server

```kotlin
val transport = StdioCodexTransport()

val client = CodexRpcClient.connect(
    CodexRpcConnection(
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
        sandbox = SandboxPolicy.WorkspaceWrite,
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

`CodexRpc` descriptors keep upstream JSON-RPC method names such as
`thread/start` and `turn/start` visible without asking callers to pass raw
strings or JSON payloads. Each descriptor binds exactly one params type to one
result type, so `CodexRpc.Thread.Start` accepts `ThreadStartParams` and returns
`ThreadStartResult`.

Identifiers and common options use lightweight SDK value types such as
`ThreadId`, `TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxPolicy`,
`ModelName`, and `TurnInput` so application code is explicit while preserving
the upstream wire shape.

## Observe Notifications

```kotlin
client.notifications.collect { notification ->
    when (notification) {
        is CodexNotification.ThreadStarted -> println(notification.threadId.value)
        is CodexNotification.ThreadStatusChanged -> println(notification.status.value)
        is CodexNotification.TurnStarted -> println(notification.turn.id.value)
        is CodexNotification.TurnCompleted -> println(notification.turn.status.value)
        is CodexNotification.TurnFailed -> println(notification.turn.error?.message)
        is CodexNotification.ItemStarted -> println(notification.item.type.value)
        is CodexNotification.ItemCompleted -> println(notification.item.status?.value)
        is CodexNotification.AgentMessageDelta -> print(notification.delta)
        is CodexNotification.ReasoningSummaryTextDelta -> print(notification.delta)
        is CodexNotification.Unknown -> println(notification.method)
    }
}
```

`notifications` exposes typed CoKit notification models. Unknown notifications
keep the method name but do not expose raw JSON through the primary API.
Turn failures are decoded from upstream `turn/completed` notifications whose
turn status is `failed`. Item lifecycle notifications expose `ThreadItemSummary`
for common rendering fields, and streamed text deltas are typed for agent
messages and reasoning summaries.

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

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

The sample defaults to `codex app-server --stdio`. Use `COKIT_CODEX_COMMAND` to
point it at another local command during development.

## Connect To app-server

```kotlin
val transport = StdioCodexTransport(
    command = listOf("codex", "app-server", "--stdio"),
)

val client = CodexAppServerClient.connect(
    CodexClientOptions(
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

## Start A Thread And Turn

```kotlin
val thread = client.threads.start(
    StartThreadRequest(
        cwd = CodexHostPath("/path/to/project"),
        approvalPolicy = ApprovalPolicy.OnRequest,
        sandbox = SandboxPolicy.WorkspaceWrite,
    ),
)

val turn = client.turns.start(
    StartTurnRequest(
        threadId = thread.id,
        input = listOf(TurnInput.Text("Summarize this repository")),
    ),
)
```

Thread and turn APIs return typed models when app-server responds with typed
payloads. Identifiers and common options use lightweight SDK value types such as
`ThreadId`, `TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxPolicy`,
`ModelName`, and `TurnInput` so application code is explicit without losing
protocol forward-compatibility. Use `TurnInput.Custom` with `CodexJsonPayload`
only when upstream has added an input variant that CoKit has not modeled yet.

## Observe Notifications

```kotlin
client.events.collect { event ->
    when (event) {
        is CodexEvent.Notification -> println(event.method)
    }
}
```

`events` exposes CoKit event types. Unknown notification payloads are preserved
as `CodexJsonPayload` so applications can keep compatibility with new upstream
members without taking a dependency on JSON-RPC envelope types.

## Handle Server Requests

CoKit defaults approval-like requests to safe responses when no handler is
registered. Consumers can register handlers for methods that need custom
decisions:

```kotlin
client.registerServerRequestHandler("item/commandExecution/requestApproval") { _ ->
    CodexServerResponse.Result(CodexJsonPayload.parse("""{"decision":"decline"}"""))
}
```

Typed approval helpers will grow from this compatibility hook as the protocol
surface is filled in.

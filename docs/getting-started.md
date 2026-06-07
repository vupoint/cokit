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
        input = emptyList(),
    ),
)
```

Thread and turn APIs return typed models when app-server responds with typed
payloads. Identifiers and common options use lightweight SDK value types such as
`ThreadId`, `TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxPolicy`, and
`ModelName` so application code is explicit without losing protocol
forward-compatibility.

## Observe Notifications

```kotlin
client.rawEvents.collect { notification ->
    println(notification.method)
}
```

`rawEvents` exposes app-server notifications directly. `events` wraps those
notifications in CoKit event types and will become the typed event surface as the
client evolves.

## Handle Server Requests

CoKit defaults approval-like requests to safe responses when no handler is
registered. Consumers can register raw handlers for methods that need custom
decisions:

```kotlin
client.registerServerRequestHandler("item/commandExecution/requestApproval") {
    buildJsonObject {
        put("decision", "decline")
    }
}
```

Typed approval helpers will grow from this raw hook as the protocol surface is
filled in.

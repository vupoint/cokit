# CoKit

CoKit is a Kotlin Multiplatform client library for `codex app-server`.

The project provides typed Kotlin APIs over the app-server JSON-RPC protocol,
with transport, protocol, RPC runtime, and high-level client concerns kept in
separate modules.

## Status

CoKit is in early development and tracks Codex CLI **0.157.1**. The current codebase includes:

- JSON-RPC protocol envelopes and serializers.
- A coroutine JSON-RPC session with response correlation and notification flows.
- A high-level app-server client initialization handshake.
- A `CodexClient` facade with typed descriptors for the currently modeled
  app-server request APIs.
- Deny-by-default handling for server-initiated approval-like requests.
- Stable section/history and attachment APIs, explicit Gateway OAuth, and richer
  thread, turn, model, MCP and managed-policy metadata.
- Opt-in experimental projects, thread queues, local user verification, and
  client-executed dynamic tools.
- JVM stdio JSONL transport.
- A guarded integration smoke test for a real local `codex app-server`.
- A schema generation Gradle workflow for app-server JSON Schema.

The upstream app-server README documents a much broader protocol surface than
CoKit currently models. See [Protocol Compatibility](docs/protocol-compatibility.md)
for the current coverage snapshot and implementation roadmap.

## Modules

Published artifacts use the `io.github.vupoint.cokit` group and include:

- `cokit-bom`: Maven BOM for aligning all published CoKit module and JVM target
  artifact versions.
- `cokit-protocol`: JSON-RPC messages, protocol serializers, and schema
  metadata. This module does not depend on client, runtime, or transport code.
- `cokit-rpc`: request correlation, notification routing, and server request
  flow. This module depends on protocol types only.
- `cokit-client-api`: public client contracts, typed request descriptors,
  notifications, server-request models, and shared option/value models.
- `cokit-client`: default `CodexClient` implementation and `CodexClients`
  factory. The raw JSON-RPC session stays internal.
- `cokit-transport-stdio`: JVM stdio transport for the local Codex app-server.
- `cokit-transport-websocket`: experimental WebSocket transport placeholder.
- `cokit-testing`: fake transports and protocol test helpers for consumers and
  CoKit module tests.

The repository also includes:

- `cokit-sample-cli`: small JVM command-line sample that exercises the public
  client and stdio transport APIs. This sample is not published as a Maven
  Central library artifact.

## Gradle Setup

CoKit artifacts are published to Maven Central. Make sure your build resolves
dependencies from `mavenCentral()`:

```kotlin
repositories {
    mavenCentral()
}
```

Import the CoKit BOM once, then declare the CoKit modules you need without
repeating the version. Replace `<version>` with the CoKit release you want to
use:

```kotlin
dependencies {
    implementation(platform("io.github.vupoint.cokit:cokit-bom:<version>"))
    implementation("io.github.vupoint.cokit:cokit-client")
    implementation("io.github.vupoint.cokit:cokit-transport-stdio")
}
```

For Groovy DSL builds, use the same coordinates:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation platform("io.github.vupoint.cokit:cokit-bom:<version>")
    implementation "io.github.vupoint.cokit:cokit-client"
    implementation "io.github.vupoint.cokit:cokit-transport-stdio"
}
```

The BOM also aligns JVM target artifacts such as `cokit-client-jvm` and
`cokit-client-api-jvm`. Gradle builds should normally use the base module
coordinates above so Gradle module metadata can select the right variant.

Use `cokit-client-api` directly only when you need the public contracts and
models without the default client implementation.

Add `cokit-testing` only to test configurations:

```kotlin
dependencies {
    testImplementation(platform("io.github.vupoint.cokit:cokit-bom:<version>"))
    testImplementation("io.github.vupoint.cokit:cokit-testing")
}
```

## Basic Example

```kotlin
val transport = StdioCodexTransport()

val client = CodexClients.connect(
    CodexClientConnection(
        transport = transport,
        clientInfo = ClientInfo(
            name = "cokit_sample",
            title = "CoKit Sample",
            version = "0.1.0",
        ),
        scope = scope,
    ),
)

val thread = client.request(
    CodexRpc.Thread.Start,
    ThreadStartParams(cwd = CodexHostPath("/path/to/project")),
).thread
val turn = client.request(
    CodexRpc.Turn.Start,
    TurnStartParams(
        threadId = thread.id,
        input = listOf(TurnInput.Text("Summarize this repository")),
    ),
).turn
```

## Dynamic Tools (Experimental)

Dynamic tools run in your application without a separate MCP server. Connect with
`InitializeCapabilities(experimentalApi = true)` in `CodexClientConnection.capabilities`,
then opt into `ExperimentalCodexApi` where you define tools and register a handler:

```kotlin
import io.github.vupoint.cokit.client.*
import io.github.vupoint.cokit.client.tools.*

@OptIn(ExperimentalCodexApi::class)
suspend fun startToolThread(client: CodexClient): Thread {
    client.registerDynamicToolCallHandler { call ->
        if (call.namespace == null && call.tool == "ping" &&
            call.arguments == CodexJsonPayload.parse("{}")) {
            DynamicToolCallResponse(
                contentItems = listOf(DynamicToolCallOutputContent.Text("pong")),
                success = true,
            )
        } else {
            DynamicToolCallResponse(contentItems = emptyList(), success = false)
        }
    }
    return client.threads.start(
        StartThreadRequest(dynamicTools = listOf(
            DynamicToolSpec.Function(
                name = "ping",
                description = "Check whether the application is responding.",
                inputSchema = CodexJsonPayload.parse(
                    """{"type":"object","properties":{},"additionalProperties":false}""",
                ),
            ),
        )),
    )
}
```

Start a turn on the returned thread to let the model use the tool. The handler
returns the result to app-server, which continues the turn. It must validate
arguments and authorize side effects before executing them. The app-server
sandbox does not sandbox application callbacks. An absent handler returns a
failure without executing a tool.

`ThreadStartParams.dynamicTools` supports the same definitions through the typed
RPC API. Namespaces use `DynamicToolSpec.Namespace` and
`DynamicToolNamespaceTool.Function`. Results support text, image URLs and audio
URLs. Existing item notifications expose dynamic tool progress and results.
Runtime tool replacement and tool-search orchestration are not provided.
See [security](docs/security.md#dynamic-tool-execution) and
[protocol compatibility](docs/protocol-compatibility.md#dynamic-tools-experimental)
for policy, lifecycle and wire details.

## Sample CLI

Show the included sample CLI help:

```bash
./gradlew :cokit-sample-cli:run --args="--help"
```

Start a real app-server thread, send the default message, and stream the
assistant response when `codex` is installed locally:

```bash
./gradlew :cokit-sample-cli:run
```

Override the default app-server host path or message when needed:

```bash
./gradlew :cokit-sample-cli:run --args='--cwd /path/to/project --message "Summarize this repository"'
```

The sample uses `StdioCodexTransport` defaults.

The command prints the created thread and turn ids, then streams assistant text
from typed `CodexNotification.AgentMessageDelta` events. If the turn completes
without assistant text, the sample reports that no assistant message was
produced; if the turn fails, it exits with the app-server error message.

## Upstream Protocol

CoKit follows the upstream app-server protocol documented in:

https://github.com/openai/codex/blob/main/codex-rs/app-server/README.md

Generated app-server JSON Schema should be treated as the source of truth for
wire shape changes.

## Verification

Run:

```bash
./gradlew check
```

`check` includes unit tests, coverage verification, module-boundary validation,
public API exposure checks, Kotlin ABI validation, and primary docs/sample
alignment checks.

Run JVM unit tests only:

```bash
./gradlew test
```

Generate aggregate Kover coverage reports:

```bash
./gradlew coverage
```

The aggregate HTML report is written to `build/reports/kover/html/index.html`.
See [Coverage](docs/coverage.md) for report paths and KMP coverage scope.

To run the real app-server integration smoke test on a machine with `codex`
installed:

```bash
COKIT_CODEX_INTEGRATION=1 ./gradlew :cokit-client:jvmTest
```

Before publishing a release candidate, follow the
[Release Readiness](docs/release-readiness.md) checklist.

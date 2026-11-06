# Client implementation boundaries

CoKit keeps public contracts in `cokit-client-api` and the default implementation
in `cokit-client`. The implementation depends on the RPC session and protocol
types; it does not depend on a concrete transport. `JsonRpcTransport` is the
existing port implemented by stdio and testing adapters. The WebSocket adapter
remains an experimental placeholder.

## Outgoing requests

`CodexClients.connect` performs the initialization handshake and constructs the
client. `DefaultCodexClient`, `DefaultThreadsApi`, and `DefaultTurnsApi` share one
internal `ClientRequestExecutor`. The executor applies the existing descriptor
and parameter capability checks, serializes parameters, calls `JsonRpcSession`,
maps the request-ID callback, and decodes the result. Thread and turn helpers
retain their request mapping and result projections.

The executor owns no coroutine scope or queue. It does not add retries or
timeouts. The request-ID callback runs after a successful transport send and
before awaiting the response. An absent result can use a descriptor's fallback;
an explicit JSON null is passed to its result serializer.

## Incoming requests and notifications

`DefaultCodexClient` owns the existing collection jobs and public observation
flows. It emits a server-request observation and then asks the internal
`ServerRequestDispatcher` to resolve the response. The dispatcher owns the typed
handlers and registration checks. It preserves the method-specific negative
defaults, human-command separation, and error/cancellation handling described in
[Security](security.md).

Observation and handler decoding remain separate. The dispatcher introduces no
new jobs, buffering, concurrency, or handler snapshot. Notification routing
remains a set of pure conversion functions.

Response builders produce internal JSON directly. Dynamic-tool output keeps a
serialization/parsing boundary so opaque content does not retain handler-owned
mutable JSON. MCP content and metadata retain their opaque-payload conversion
boundary. Public payload wrappers and serializers remain in the API module.

## Maintenance checks

Keep implementation details internal. Changes to request execution or dispatch
should exercise the public client through a fake transport, asserting wire
payloads, results, callback ordering, and handler effects. The client tests also
cover initialization cleanup, malformed unhandled requests, null distinctions,
and response payload ownership.

Run `./gradlew :cokit-client:check` for client changes and `./gradlew check` before
delivery. These checks include ABI validation and the existing module and public
API boundary checks. Do not update ABI reference dumps to accommodate an internal
refactoring. Schema assumptions and fixture provenance follow
[Protocol compatibility](protocol-compatibility.md).

Session shutdown, transport termination, buffer overflow, raw framing limits,
and handler cancellation policy are separate behavior changes. The internal
client structure does not alter their current semantics or implement additional
transport reliability guarantees.

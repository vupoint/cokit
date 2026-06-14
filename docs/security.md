# Security

CoKit is a client library for an app-server that can request filesystem access,
command execution, permission changes, and other sensitive operations. Consumers
must treat app-server requests as security-sensitive.

## Approval Defaults

When no handler is registered, CoKit declines approval-like server requests by
default. This prevents a client from silently allowing command execution, file
changes, permission grants, dynamic tool calls, or elicitation flows.

Applications should register explicit handlers only when they can show the
request to a user, apply a policy, or otherwise make an intentional decision.
Prefer typed handlers such as command approval handlers over raw method-string
compatibility hooks.

## Host Semantics

Paths, commands, process APIs, and filesystem APIs refer to the app-server host.
That host may be different from the Kotlin client process host in remote-control
or proxy scenarios.

## Secrets

CoKit should not log secrets by default. Consumers should avoid logging:

- API keys.
- Access tokens.
- Auth URLs.
- Attestation tokens.
- Private file paths when logs may leave the machine.

## Sandbox And Permissions

Sandbox and permission profile values are passed through to app-server. CoKit
does not weaken or reinterpret upstream sandbox semantics. Application-level
policy should make approval decisions explicit and visible.

## Experimental Transports

The WebSocket transport is experimental. Use stdio for the default local
integration path unless an application has a specific reason to opt into
WebSocket behavior.

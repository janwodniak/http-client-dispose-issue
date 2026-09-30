### Mocked External Service

To reproduce the issue, a list of transactions was mocked using Postman in JSON format.

Additionally, a 1-second delay was added to the response:

```shell
curl -X GET https://fa218db5-883c-4799-8802-c4cbba1f554f.mock.pstmn.io/transactions
```

The underlying protocol used is HTTP/2:

```shell
curl -I https://fa218db5-883c-4799-8802-c4cbba1f554f.mock.pstmn.io/transactions
```

### Observed Issue

When a `Reactor HTTP client` method returning a `Flux` is invoked, subscribing to the `Flux` and then calling
`dispose()`
on the resulting `Disposable` causes the client to hang, preventing it from handling later requests.

In contrast, calling `dispose()` on a `Mono` does not exhibit this issue.

### Controller for Reproducing the Issue

To observe this behavior, a controller named `TransactionsRespController` was created with four endpoints:

- `/transactions/flux` - Returns a `Flux<Transaction>`.
- `/transactions/flux/dispose` - Subscribes to a `Flux<Transaction>` and then calls `dispose()`.
- `/transactions/mono` - Returns a `Mono<List<Transaction>>`.
- `/transactions/mono/dispose` - Subscribes to a `Mono<List<Transaction>>` and then calls `dispose()`.

### Guide for Issue Trigger

1. **Invoke `/transactions/flux` and `/transactions/mono`** – These calls will succeed normally, demonstrating that
   both the `Flux` and `Mono` endpoints work as expected.

2. **Invoke `/transactions/flux/dispose`** – Typically, invoking this endpoint once is enough to trigger the hanging
   behavior and subsequent timeouts.

3. **Invoke `/transactions/flux` or `/transactions/mono` again** – At this point, the client hangs, and any further
   requests will not proceed.

A file named `transactions-rest-controller.http` in the `resources` directory contains all of these requests for
convenient testing.

### Starting the Micronaut Application

To start the application, run the following command:

```shell
./gradlew run
```

By default, Micronaut will start the embedded HTTP server on port **8080**.

### Root Cause

The hang is a bug in the Micronaut HTTP client up to 4.7.x (reproduced with `micronaut-http-client` 4.7.13 from
platform 4.7.5).

`DefaultHttpClient.sendRawRequest` sent the request from inside `Mono.create(...)` but never registered an
`onCancel`/`onDispose` handler. When the subscription is cancelled after the request is written but before the
response arrives, which is what `flux.subscribe()` followed immediately by `dispose()` does:

1. The response is dropped, and its streaming body is never read or closed.
2. The pool handle is never released. It is only released once the body is fully consumed.
3. On HTTP/2, the orphaned stream keeps the body bytes it received. Those bytes count against the
   **connection-level** flow-control window, which Netty only replenishes when a stream consumes its data. The default
   window is 64 KB. The mocked response is about 140 KB, so a single orphaned stream can use up the whole window.
4. Every later request multiplexed on the same HTTP/2 connection gets its response headers but never its body, so the
   client hangs until the read timeout.

The `Mono` endpoint is not affected because the full-body path buffers and releases the response eagerly. With a
small response body that fits in the window (for example 5 transactions), the `Flux` endpoint does not hang either.

### Fix

Upgrade Micronaut. In 4.10.x the raw request path was rewritten on top of `ExecutionFlow` with proper cancellation
handling. This project now uses `micronautVersion=4.10.20` (`micronaut-http-client` 4.10.30). With it, 10+
consecutive calls to `/transactions/flux/dispose` leave the client fully working. On 4.7.5 the same sequence
reliably hangs the client.

### Reproducing Locally (without the Postman mock)

Any HTTP/2 (TLS + ALPN) server that returns a JSON array larger than 64 KB after a short delay will do, for example
`hypercorn` serving `src/main/resources/transactions.json` with a 1 s sleep. Point the client at it with:

```shell
JAVA_OPTS="-Dmicronaut.http.services.mocked-transactions.url=https://127.0.0.1:8443/transactions \
  -Dmicronaut.http.services.mocked-transactions.ssl.enabled=true \
  -Dmicronaut.http.services.mocked-transactions.ssl.insecure-trust-all-certificates=true" \
  build/install/http-client-dispose-issue/bin/http-client-dispose-issue
```

Then call `/transactions/flux/dispose` several times, followed by `/transactions/flux` or `/transactions/mono`.

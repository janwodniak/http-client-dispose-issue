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

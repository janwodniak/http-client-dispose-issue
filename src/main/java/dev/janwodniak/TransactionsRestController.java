package dev.janwodniak;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller("/transactions")
class TransactionsRestController {
    private final MockedTransactionsApi mockedTransactionsApi;

    TransactionsRestController(MockedTransactionsApi mockedTransactionsApi) {
        this.mockedTransactionsApi = mockedTransactionsApi;
    }

    @Get("/flux")
    Flux<Transaction> transactionsAsFlux() {
        return mockedTransactionsApi.transactionsAsFlux().log();
    }

    @Get("/flux/dispose")
    Mono<String> transactionsAsFluxThenDispose() {
        Flux<Transaction> flux = mockedTransactionsApi.transactionsAsFlux().log();
        Disposable disposable = flux.subscribe();
        disposable.dispose();
        return Mono.just("Disposed %s".formatted(disposable.getClass()));
    }

    @Get("/mono")
    Mono<List<Transaction>> transactionsAsMono() {
        return mockedTransactionsApi.transactionsAsMono().log();
    }

    @Get("/mono/dispose")
    Mono<String> transactionsAsMonoThenDispose() {
        Mono<List<Transaction>> mono = mockedTransactionsApi.transactionsAsMono().log();
        Disposable disposable = mono.subscribe();
        disposable.dispose();
        return Mono.just("Disposed %s".formatted(disposable.getClass()));
    }
}

package dev.janwodniak;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Client(id = "mocked-transactions", path = "/transactions")
interface MockedTransactionsApi {
    @Get
    Mono<List<Transaction>> transactionsAsMono();

    @Get
    Flux<Transaction> transactionsAsFlux();
}

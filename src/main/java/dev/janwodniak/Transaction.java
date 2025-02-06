package dev.janwodniak;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

@Serdeable
record Transaction(
    String id,
    String description,
    BigDecimal amount,
    String currency
) {
}

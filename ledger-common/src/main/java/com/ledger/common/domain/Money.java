package com.ledger.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final Currency currency;
    private Money(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "Amount não pode ser nulo");
        Objects.requireNonNull(currency, "Currency não pode ser nula");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money não pode ser negativo: " + amount);
        }
        this.amount = amount.setScale(4, RoundingMode.HALF_EVEN);
        this.currency = currency;
    }
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    public static Money ofBRL(BigDecimal amount) {
        return of(amount, "BRL");
    }
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtração resultaria em valor negativo");
        }
        return new Money(result, this.currency);
    }
    public boolean isGreaterThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }
    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Moedas incompatíveis: " + this.currency + " vs " + other.currency
            );
        }
    }
    public BigDecimal getAmount()   { return amount; }
    public String getCurrencyCode() { return currency.getCurrencyCode(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money m)) return false;
        return amount.compareTo(m.amount) == 0 && currency.equals(m.currency);
    }
    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
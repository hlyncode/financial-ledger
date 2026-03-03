package com.ledger.common.domain; 
import java.util.Objects;
import java.util.UUID;

public final class SagaId {
    private final UUID value; 
    private SagaId(UUID value) { 
        Objects.requireNonNull(value, "SagaId não pode ser nulo"); 
        this.value = value;
    } 
    public static SagaId of(UUID value) {
        return new SagaId(value);
    } 
    public static SagaId of(String value) { 
        try {
            return new SagaId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("SagaId inválido: " + value, e);
        }
    } 
    public static SagaId generate() {
        return new SagaId(UUID.randomUUID());
    } 
    public UUID getValue() {
        return value;
    } 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; 
        if (!(o instanceof SagaId s)) return false; 
        return value.equals(s.value);
    }
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    @Override
    public String toString() {
        return value.toString();
    }
}
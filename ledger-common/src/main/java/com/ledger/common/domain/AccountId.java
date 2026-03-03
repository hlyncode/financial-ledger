package com.ledger.common.domain;
import java.util.Objects;
import java.util.UUID;

public final class AccountId {
    private final UUID value;
    private AccountId(UUID value) { 
        Objects.requireNonNull(value, "AccountId não pode ser nulo"); 
        this.value = value; 
    } 
    public static AccountId of(UUID value) { 
        return new AccountId(value);
    } 
    //conveniência para receber string vinda da api ou do banco
    public static AccountId of(String value) {
        try {
            return new AccountId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("AccountId inválido: " + value);
        }
    } 
    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    } 
    public UUID getValue() {
        return value;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId a)) return false;
        return value.equals(a.value);
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
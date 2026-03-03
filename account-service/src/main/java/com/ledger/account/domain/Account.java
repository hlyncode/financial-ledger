package com.ledger.account.domain;

import com.ledger.common.domain.AccountId;
import com.ledger.common.domain.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor; 
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid") 
    private UUID ownerId;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private java.math.BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency; 

    @Column(nullable = false)
    private String status;

    @Version //optimistic locking: se dois processos tentarem salvar ao mesmo tempo, um vai falhar 
    private long version; 

    @Column(name = "created_at") 
    private OffsetDateTime createdAt;

    @Column(name = "updated_at") 
    private OffsetDateTime updatedAt;

    //factory method: única forma de criar uma conta nova
    
    public static Account create(UUID ownerId, String accountNumber, Money initialBalance) {
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.ownerId = ownerId;
        account.accountNumber = accountNumber; 
        account.balance = initialBalance.getAmount();
        account.currency = initialBalance.getCurrencyCode();
        account.status = "ACTIVE";
        account.createdAt = OffsetDateTime.now ();
        account.updatedAt = OffsetDateTime.now ();
        return account;
    }

    public void debit(Money amount) {
        assertActive();
        Money currentBalance = Money.of(this.balance, this.currency);
        if (!currentBalance.isGreaterThanOrEqual(amount)) {
            throw new InsufficientFundsException(
                "Saldo insuficiente na conta " + accountNumber +
                ". Saldo: " + currentBalance + ", Tentativa de débito: " + amount
            );
    } 
        this.balance = currentBalance.subtract(amount).getAmount();
        this.updatedAt = OffsetDateTime.now();
}
    
    public void credit(Money amount) { 
        assertActive(); 
        Money currentBalance = Money.of(this.balance, this.currency); 
        this.balance = currentBalance.add(amount).getAmount();
        this.updatedAt = OffsetDateTime.now();
    }
    public Money getBalanceAsMoney() {
        return Money.of(this.balance, this.currency);
    }
    public AccountId getAccountId() {
        return AccountId.of(this.id);
    }

    private void assertActive() {
        if(!"ACTIVE".equals(this.status)) {
            throw new AccountNotActiveException(
                "Conta " + accountNumber + " não está ativa. Status: " + status
            );
        }
    }
}
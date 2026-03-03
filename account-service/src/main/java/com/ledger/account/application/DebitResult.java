package com.ledger.account.application;

import com.ledger.common.domain.AccountId;
import com.ledger.common.domain.Money;

public record DebitResult (
        boolean success,
        AccountId accountId,
        Money newBalance,
        String failureReason
) {
    public static DebitResult success(AccountId accountId, Money newBalance) {
        return new DebitResult(true, accountId, newBalance, null);
    }
    public static DebitResult failure(String reason) {
        return new DebitResult(false, null, null, reason);
    }
}
package com.ledger.account.application; 

import com.ledger.common.domain.AccountId;
import com.ledger.common.domain.Money;

public record CreditResult(
    boolean success,
    AccountId accountId,
    Money newBalance,
    String failureReason
) {
    public static CreditResult success(AccountId accountId, Money newBalance) {
        return new CreditResult(true, accountId, newBalance, null);
    }
    public static CreditResult failure(String reason) {
        return new CreditResult(false, null, null, reason);
    }
}
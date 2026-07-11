package com.firstclub.membership.web.dto.response;

import com.firstclub.membership.domain.pricing.Money;

import java.math.BigDecimal;

/** Wire representation of {@link Money}: amount + ISO currency. */
public record MoneyDto(BigDecimal amount, String currency) {

    public static MoneyDto from(Money money) {
        return money == null ? null : new MoneyDto(money.getAmount(), money.getCurrency());
    }
}

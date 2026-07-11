package com.firstclub.membership.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Demo hook: push an order into a user's stats so tier progression can be exercised.
 *
 * @param userId     the user who placed the order
 * @param amount     order value (must be positive)
 * @param occurredAt when the order happened; defaults to now if omitted
 */
public record RecordOrderRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount must be positive") BigDecimal amount,
        Instant occurredAt) {
}

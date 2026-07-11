package com.firstclub.membership.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Subscribe a user to a plan at a tier.
 *
 * @param userId   the subscribing user
 * @param planCode plan code: {@code MONTHLY} | {@code QUARTERLY} | {@code YEARLY}
 * @param tierCode tier code: {@code SILVER} | {@code GOLD} | {@code PLATINUM}
 */
public record SubscribeRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotBlank(message = "planCode is required") String planCode,
        @NotBlank(message = "tierCode is required") String tierCode) {
}

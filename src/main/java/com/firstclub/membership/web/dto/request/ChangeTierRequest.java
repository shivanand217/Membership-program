package com.firstclub.membership.web.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Change the tier of an existing subscription (used by both upgrade and downgrade).
 *
 * @param targetTierCode target tier: {@code SILVER} | {@code GOLD} | {@code PLATINUM}
 */
public record ChangeTierRequest(
        @NotBlank(message = "targetTierCode is required") String targetTierCode) {
}

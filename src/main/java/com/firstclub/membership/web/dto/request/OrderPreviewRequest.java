package com.firstclub.membership.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Apply a member's configured benefits to a sample order and see the resulting savings.
 *
 * @param userId      member whose effective tier's benefits apply
 * @param subtotal    order value before benefits
 * @param deliveryFee delivery fee (free-delivery may waive it); defaults to 0
 * @param category    item category for category-scoped discounts (optional)
 */
public record OrderPreviewRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotNull(message = "subtotal is required")
        @DecimalMin(value = "0.0", message = "subtotal cannot be negative") BigDecimal subtotal,
        @DecimalMin(value = "0.0", message = "deliveryFee cannot be negative") BigDecimal deliveryFee,
        String category) {
}

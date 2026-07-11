package com.firstclub.membership.domain.vo;

import com.firstclub.membership.domain.pricing.Money;

/**
 * A sample order fed into the benefit-preview endpoint so we can demonstrate the configured perks
 * (percentage discounts, free delivery) acting on a real basket. Not persisted.
 *
 * @param subtotal    order value before member benefits
 * @param deliveryFee delivery fee that free-delivery may waive
 * @param category    item category used to scope category-specific discounts (nullable = generic)
 */
public record OrderPreview(Money subtotal, Money deliveryFee, String category) {

    public OrderPreview {
        if (subtotal == null) {
            throw new IllegalArgumentException("subtotal is required");
        }
        if (deliveryFee == null) {
            deliveryFee = Money.zero(subtotal.getCurrency());
        }
    }

    public String categoryOrAll() {
        return (category == null || category.isBlank()) ? "all" : category;
    }
}

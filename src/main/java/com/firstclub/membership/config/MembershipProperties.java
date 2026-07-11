package com.firstclub.membership.config;

import com.firstclub.membership.domain.pricing.Money;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.ZoneId;

/**
 * Strongly-typed binding of the {@code membership.*} configuration. Centralising these makes the
 * business rules (which timezone defines "a month", the global discount cap, the sweep cadence)
 * configuration rather than magic constants scattered through the code.
 */
@ConfigurationProperties(prefix = "membership")
public class MembershipProperties {

    /** Single business timezone that defines the "per month" window for monthly tier criteria. */
    private String businessZone = "Asia/Kolkata";

    /** ISO currency used across the platform (single-currency for this assignment). */
    private String currency = "INR";

    private final Discount discount = new Discount();
    private final Expiry expiry = new Expiry();

    public ZoneId businessZoneId() {
        return ZoneId.of(businessZone);
    }

    public Money globalDiscountCap() {
        return Money.of(discount.getGlobalMax(), currency);
    }

    public String getBusinessZone() {
        return businessZone;
    }

    public void setBusinessZone(String businessZone) {
        this.businessZone = businessZone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Discount getDiscount() {
        return discount;
    }

    public Expiry getExpiry() {
        return expiry;
    }

    /** Order-preview discount policy. */
    public static class Discount {
        /** Single global cap (currency units) on the sum of stacked percentage discounts. */
        private BigDecimal globalMax = new BigDecimal("1500");

        public BigDecimal getGlobalMax() {
            return globalMax;
        }

        public void setGlobalMax(BigDecimal globalMax) {
            this.globalMax = globalMax;
        }
    }

    /** Lifecycle sweep configuration. */
    public static class Expiry {
        /** Cron for the expire/renew/apply-deferred-downgrade sweep. */
        private String sweepCron = "0 * * * * *";
        private boolean enabled = true;

        public String getSweepCron() {
            return sweepCron;
        }

        public void setSweepCron(String sweepCron) {
            this.sweepCron = sweepCron;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

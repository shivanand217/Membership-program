package com.firstclub.membership.web.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Full view of a subscription after a lifecycle operation, including the earned/selected/effective tier
 * split, the audit event log and the billing ledger — so a reviewer can see exactly what happened.
 */
public record SubscriptionResponse(Long id,
                                   Long userId,
                                   String planCode,
                                   String billingCycle,
                                   String status,
                                   String selectedTier,
                                   String earnedTier,
                                   String effectiveTier,
                                   String pendingTier,
                                   MoneyDto pricePaid,
                                   Instant startsAt,
                                   Instant endsAt,
                                   boolean autoRenew,
                                   boolean cancelAtPeriodEnd,
                                   List<EventView> events,
                                   List<TransactionView> transactions) {

    public record EventView(String changeType,
                            String fromTier,
                            String toTier,
                            String fromStatus,
                            String toStatus,
                            String reason,
                            Instant occurredAt) {
    }

    public record TransactionView(String kind,
                                  MoneyDto amount,
                                  String changeType,
                                  Instant periodStart,
                                  Instant occurredAt,
                                  String note) {
    }
}

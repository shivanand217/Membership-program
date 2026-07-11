package com.firstclub.membership.domain.enums;

/**
 * Direction of a money movement recorded in the {@code BillingTransaction} ledger.
 */
public enum BillingKind {
    /** Money owed by / charged to the member (subscribe, renew, prorated upgrade). */
    CHARGE,
    /** Account credit issued to the member. */
    CREDIT,
    /** Money returned to the member. */
    REFUND
}

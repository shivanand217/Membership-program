package com.firstclub.membership.domain.enums;

/**
 * Classifies an entry in a subscription's append-only event log. Auto-promote/demote are system-driven
 * (criteria) and never re-price; the rest are user- or scheduler-driven.
 */
public enum ChangeType {
    SUBSCRIBE,
    RENEW,
    UPGRADE,
    DOWNGRADE,
    CANCEL,
    EXPIRE,
    AUTO_PROMOTE,
    AUTO_DEMOTE
}

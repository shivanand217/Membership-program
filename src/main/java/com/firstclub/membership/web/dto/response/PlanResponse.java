package com.firstclub.membership.web.dto.response;

/** A membership plan the user can pick. */
public record PlanResponse(String code, String name, String billingCycle, MoneyDto price) {
}

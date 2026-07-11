package com.firstclub.membership.web.controller;

import com.firstclub.membership.application.SubscriptionService;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.web.dto.request.ChangeTierRequest;
import com.firstclub.membership.web.dto.request.SubscribeRequest;
import com.firstclub.membership.web.dto.response.SubscriptionResponse;
import com.firstclub.membership.web.mapper.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscribe, upgrade, downgrade, cancel")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final DtoMapper mapper;

    public SubscriptionController(SubscriptionService subscriptionService, DtoMapper mapper) {
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Subscribe to a plan + tier",
            description = "At most one active subscription per user. Pass an optional Idempotency-Key header to make retries safe.")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Subscription subscription = subscriptionService.subscribe(
                request.userId(), request.planCode(), request.tierCode(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toSubscriptionResponse(subscription));
    }

    @PostMapping("/{id}/upgrade")
    @Operation(summary = "Upgrade tier (immediate, prorated)")
    public SubscriptionResponse upgrade(@PathVariable Long id, @Valid @RequestBody ChangeTierRequest request) {
        return mapper.toSubscriptionResponse(subscriptionService.upgrade(id, request.targetTierCode()));
    }

    @PostMapping("/{id}/downgrade")
    @Operation(summary = "Downgrade tier (applied at period end)")
    public SubscriptionResponse downgrade(@PathVariable Long id, @Valid @RequestBody ChangeTierRequest request) {
        return mapper.toSubscriptionResponse(subscriptionService.downgrade(id, request.targetTierCode()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel (at period end; benefits run to expiry)")
    public SubscriptionResponse cancel(@PathVariable Long id) {
        return mapper.toSubscriptionResponse(subscriptionService.cancel(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subscription with its full event log and billing ledger")
    public SubscriptionResponse get(@PathVariable Long id) {
        return mapper.toSubscriptionResponse(subscriptionService.getSubscription(id));
    }
}

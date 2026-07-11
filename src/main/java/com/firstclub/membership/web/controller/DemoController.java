package com.firstclub.membership.web.controller;

import com.firstclub.membership.application.BenefitEvaluationService;
import com.firstclub.membership.application.OrderStatsService;
import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.model.UserOrderStats;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.domain.vo.OrderPreview;
import com.firstclub.membership.web.dto.request.OrderPreviewRequest;
import com.firstclub.membership.web.dto.request.RecordOrderRequest;
import com.firstclub.membership.web.dto.response.BenefitPreviewResponse;
import com.firstclub.membership.web.dto.response.OrderStatsView;
import com.firstclub.membership.web.mapper.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Demo-support endpoints that stand in for the wider shopping platform: feeding orders into a user's
 * stats (to drive tier progression) and previewing member benefits against a sample basket.
 */
@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Demo", description = "Feed orders and preview member benefits on a sample cart")
public class DemoController {

    private final OrderStatsService orderStats;
    private final BenefitEvaluationService benefits;
    private final DtoMapper mapper;
    private final MembershipProperties properties;

    public DemoController(OrderStatsService orderStats,
                          BenefitEvaluationService benefits,
                          DtoMapper mapper,
                          MembershipProperties properties) {
        this.orderStats = orderStats;
        this.benefits = benefits;
        this.mapper = mapper;
        this.properties = properties;
    }

    @PostMapping("/orders")
    @Operation(summary = "Record an order", description = "Adds to the user's lifetime and monthly stats")
    public OrderStatsView recordOrder(@Valid @RequestBody RecordOrderRequest request) {
        Instant when = request.occurredAt() != null ? request.occurredAt() : Instant.now();
        Money amount = Money.of(request.amount(), properties.getCurrency());
        UserOrderStats stats = orderStats.recordOrder(request.userId(), amount, when);
        return mapper.toOrderStatsView(stats, orderStats.currentBusinessMonth(), properties.getCurrency());
    }

    @PostMapping("/benefits/preview")
    @Operation(summary = "Preview benefits on a cart",
            description = "Applies the member's effective-tier benefits to a sample order")
    public BenefitPreviewResponse previewBenefits(@Valid @RequestBody OrderPreviewRequest request) {
        String currency = properties.getCurrency();
        Money subtotal = Money.of(request.subtotal(), currency);
        Money deliveryFee = request.deliveryFee() != null
                ? Money.of(request.deliveryFee(), currency)
                : Money.zero(currency);
        OrderPreview order = new OrderPreview(subtotal, deliveryFee, request.category());
        return mapper.toBenefitPreviewResponse(benefits.preview(request.userId(), order));
    }
}

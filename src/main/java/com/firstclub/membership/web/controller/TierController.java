package com.firstclub.membership.web.controller;

import com.firstclub.membership.application.MembershipCatalogService;
import com.firstclub.membership.web.dto.response.TierResponse;
import com.firstclub.membership.web.mapper.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tiers")
@Tag(name = "Tiers", description = "Membership tiers with their configurable benefits and progression criteria")
public class TierController {

    private final MembershipCatalogService catalog;
    private final DtoMapper mapper;

    public TierController(MembershipCatalogService catalog, DtoMapper mapper) {
        this.catalog = catalog;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List tiers", description = "Each tier with its benefits (and params) and criteria")
    public List<TierResponse> listTiers() {
        return catalog.listActiveTiers().stream().map(mapper::toTierResponse).toList();
    }

    @GetMapping("/{code}/benefits")
    @Operation(summary = "List a tier's benefits", description = "code = SILVER | GOLD | PLATINUM")
    public List<TierResponse.BenefitView> tierBenefits(
            @Parameter(description = "Tier code: SILVER, GOLD or PLATINUM") @PathVariable String code) {
        return mapper.toBenefitViews(catalog.requireTierByLevelCode(code));
    }
}

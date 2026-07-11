package com.firstclub.membership.web.controller;

import com.firstclub.membership.application.MembershipCatalogService;
import com.firstclub.membership.web.dto.response.PlanResponse;
import com.firstclub.membership.web.mapper.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Available membership plans (billing cadence + price)")
public class PlanController {

    private final MembershipCatalogService catalog;
    private final DtoMapper mapper;

    public PlanController(MembershipCatalogService catalog, DtoMapper mapper) {
        this.catalog = catalog;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List active plans", description = "Monthly / Quarterly / Yearly plans with pricing")
    public List<PlanResponse> listPlans() {
        return catalog.listActivePlans().stream().map(mapper::toPlanResponse).toList();
    }
}

package com.firstclub.membership.config;

import com.firstclub.membership.application.OrderStatsService;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.enums.CriteriaMatch;
import com.firstclub.membership.domain.enums.CriteriaType;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.AppUser;
import com.firstclub.membership.domain.model.Benefit;
import com.firstclub.membership.domain.model.Cohort;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.domain.model.UserCohortMembership;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.repository.AppUserRepository;
import com.firstclub.membership.repository.BenefitRepository;
import com.firstclub.membership.repository.CohortRepository;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.TierRepository;
import com.firstclub.membership.repository.UserCohortMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.firstclub.membership.domain.enums.BillingCycle.MONTHLY;
import static com.firstclub.membership.domain.enums.BillingCycle.QUARTERLY;
import static com.firstclub.membership.domain.enums.BillingCycle.YEARLY;

/**
 * Seeds a self-describing demo dataset on startup: the plan/tier/benefit catalog plus four users whose
 * activity exercises every tier-progression path. Idempotent — skips if data already exists.
 *
 * <p>Demo users (ids 1..4):
 * <ul>
 *   <li><b>u1</b> — no activity → stays Silver.</li>
 *   <li><b>u2</b> — 4 lifetime orders, ₹1800 this month → Silver now; one more order crosses to Gold (order count).</li>
 *   <li><b>u3</b> — member of {@code prime_metro} → earns Platinum by cohort alone (no orders).</li>
 *   <li><b>u4</b> — 12 orders / ₹12000 this month → earns Platinum by monthly volume.</li>
 * </ul>
 */
@Component
@Order(10)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final MembershipPlanRepository planRepository;
    private final BenefitRepository benefitRepository;
    private final TierRepository tierRepository;
    private final CohortRepository cohortRepository;
    private final AppUserRepository userRepository;
    private final UserCohortMembershipRepository cohortMembershipRepository;
    private final OrderStatsService orderStatsService;

    public DataSeeder(MembershipPlanRepository planRepository,
                      BenefitRepository benefitRepository,
                      TierRepository tierRepository,
                      CohortRepository cohortRepository,
                      AppUserRepository userRepository,
                      UserCohortMembershipRepository cohortMembershipRepository,
                      OrderStatsService orderStatsService) {
        this.planRepository = planRepository;
        this.benefitRepository = benefitRepository;
        this.tierRepository = tierRepository;
        this.cohortRepository = cohortRepository;
        this.userRepository = userRepository;
        this.cohortMembershipRepository = cohortMembershipRepository;
        this.orderStatsService = orderStatsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            log.info("Seed data already present — skipping seeding");
            return;
        }
        seedPlans();
        Map<BenefitType, Benefit> benefits = seedBenefitCatalog();
        seedTiers(benefits);
        seedCohorts();
        seedUsersAndActivity();
        log.info("Seeded plans, tiers, benefits, cohorts and 4 demo users (ids 1-4)");
    }

    private void seedPlans() {
        planRepository.save(new MembershipPlan("MONTHLY", "Monthly", MONTHLY, Money.inr(199)));
        planRepository.save(new MembershipPlan("QUARTERLY", "Quarterly", QUARTERLY, Money.inr(499)));
        planRepository.save(new MembershipPlan("YEARLY", "Yearly", YEARLY, Money.inr(1499)));
    }

    private Map<BenefitType, Benefit> seedBenefitCatalog() {
        Benefit freeDelivery = benefitRepository.save(new Benefit("FREE_DELIVERY", BenefitType.FREE_DELIVERY,
                "Free Delivery", "Waive the delivery fee on eligible orders"));
        Benefit percentageDiscount = benefitRepository.save(new Benefit("PERCENTAGE_DISCOUNT", BenefitType.PERCENTAGE_DISCOUNT,
                "Extra Discount", "Extra percentage off, optionally scoped to categories"));
        Benefit exclusiveDeals = benefitRepository.save(new Benefit("EXCLUSIVE_DEALS", BenefitType.EXCLUSIVE_DEALS,
                "Exclusive Deals", "Access to members-only deals"));
        Benefit earlyAccess = benefitRepository.save(new Benefit("EARLY_ACCESS_SALES", BenefitType.EARLY_ACCESS_SALES,
                "Early Access", "Early access to sales windows"));
        Benefit prioritySupport = benefitRepository.save(new Benefit("PRIORITY_SUPPORT", BenefitType.PRIORITY_SUPPORT,
                "Priority Support", "Priority customer support with an SLA"));
        Benefit coupons = benefitRepository.save(new Benefit("EXCLUSIVE_COUPONS", BenefitType.EXCLUSIVE_COUPONS,
                "Exclusive Coupons", "A set of exclusive coupons"));

        return Map.of(
                BenefitType.FREE_DELIVERY, freeDelivery,
                BenefitType.PERCENTAGE_DISCOUNT, percentageDiscount,
                BenefitType.EXCLUSIVE_DEALS, exclusiveDeals,
                BenefitType.EARLY_ACCESS_SALES, earlyAccess,
                BenefitType.PRIORITY_SUPPORT, prioritySupport,
                BenefitType.EXCLUSIVE_COUPONS, coupons);
    }

    private void seedTiers(Map<BenefitType, Benefit> b) {
        // SILVER — base tier (rank 0, no criteria, always eligible), a small everyday discount.
        Tier silver = new Tier(TierLevel.SILVER, "Silver", 0, new BigDecimal("1.00"),
                "Entry tier — everyday savings");
        silver.addBenefit(new TierBenefit(b.get(BenefitType.PERCENTAGE_DISCOUNT), true, 10,
                Map.of("percentage", 5, "maxDiscount", 100, "categories", List.of("all"))));
        tierRepository.save(silver);

        // GOLD — earn by any of: 5 lifetime orders, ₹2000/month, or early_adopter cohort.
        Tier gold = new Tier(TierLevel.GOLD, "Gold", 1, new BigDecimal("1.50"),
                "Mid tier — free delivery + bigger discounts").setCriteriaMatch(CriteriaMatch.ANY);
        gold.addBenefit(new TierBenefit(b.get(BenefitType.FREE_DELIVERY), true, 5,
                Map.of("minOrderValue", 500)));
        gold.addBenefit(new TierBenefit(b.get(BenefitType.PERCENTAGE_DISCOUNT), true, 10,
                Map.of("percentage", 10, "maxDiscount", 300, "categories", List.of("grocery", "fashion"))));
        gold.addBenefit(new TierBenefit(b.get(BenefitType.EXCLUSIVE_DEALS), true, 20, Map.of()));
        gold.addCriterion(TierCriterion.numeric(CriteriaType.MIN_LIFETIME_ORDERS, new BigDecimal("5")));
        gold.addCriterion(TierCriterion.numeric(CriteriaType.MIN_MONTHLY_ORDER_VALUE, new BigDecimal("2000")));
        gold.addCriterion(TierCriterion.cohort("early_adopter"));
        tierRepository.save(gold);

        // PLATINUM — earn by any of: 10 orders/month, ₹10000/month, or prime_metro cohort.
        Tier platinum = new Tier(TierLevel.PLATINUM, "Platinum", 2, new BigDecimal("2.00"),
                "Top tier — max discounts, priority support, coupons").setCriteriaMatch(CriteriaMatch.ANY);
        platinum.addBenefit(new TierBenefit(b.get(BenefitType.FREE_DELIVERY), true, 5,
                Map.of("minOrderValue", 0)));
        platinum.addBenefit(new TierBenefit(b.get(BenefitType.PERCENTAGE_DISCOUNT), true, 10,
                Map.of("percentage", 15, "maxDiscount", 1000, "categories", List.of("all"))));
        platinum.addBenefit(new TierBenefit(b.get(BenefitType.EXCLUSIVE_DEALS), true, 20, Map.of()));
        platinum.addBenefit(new TierBenefit(b.get(BenefitType.EARLY_ACCESS_SALES), true, 30,
                Map.of("earlyAccessHours", 48)));
        platinum.addBenefit(new TierBenefit(b.get(BenefitType.PRIORITY_SUPPORT), true, 40,
                Map.of("slaMinutes", 15, "channel", "phone")));
        platinum.addBenefit(new TierBenefit(b.get(BenefitType.EXCLUSIVE_COUPONS), true, 50,
                Map.of("count", 3)));
        platinum.addCriterion(TierCriterion.numeric(CriteriaType.MIN_MONTHLY_ORDERS, new BigDecimal("10")));
        platinum.addCriterion(TierCriterion.numeric(CriteriaType.MIN_MONTHLY_ORDER_VALUE, new BigDecimal("10000")));
        platinum.addCriterion(TierCriterion.cohort("prime_metro"));
        tierRepository.save(platinum);
    }

    private void seedCohorts() {
        cohortRepository.save(new Cohort("early_adopter", "Early Adopter", "Joined during launch — a Gold path"));
        cohortRepository.save(new Cohort("prime_metro", "Prime Metro", "Premium metro segment — a Platinum path"));
    }

    private void seedUsersAndActivity() {
        AppUser u1 = userRepository.save(new AppUser("u1", "u1@firstclub.test", "Baseline User"));
        AppUser u2 = userRepository.save(new AppUser("u2", "u2@firstclub.test", "Near-Gold User"));
        AppUser u3 = userRepository.save(new AppUser("u3", "u3@firstclub.test", "Prime Metro Member"));
        AppUser u4 = userRepository.save(new AppUser("u4", "u4@firstclub.test", "High-Volume User"));

        // u3 earns Platinum purely by cohort membership.
        Cohort primeMetro = cohortRepository.findByCode("prime_metro").orElseThrow();
        cohortMembershipRepository.save(new UserCohortMembership(u3.getId(), primeMetro));

        // u2: 4 orders totalling ₹1800 this month (Silver — just short of Gold on both count and value).
        for (int i = 0; i < 4; i++) {
            orderStatsService.recordOrder(u2.getId(), Money.inr(450), Instant.now());
        }
        // u4: 12 orders totalling ₹12000 this month (comfortably Platinum).
        for (int i = 0; i < 12; i++) {
            orderStatsService.recordOrder(u4.getId(), Money.inr(1000), Instant.now());
        }
    }
}

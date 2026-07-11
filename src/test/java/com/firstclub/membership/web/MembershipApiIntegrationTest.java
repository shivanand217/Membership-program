package com.firstclub.membership.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests over the real HTTP surface (MockMvc) against the seeded demo dataset. Each test uses a
 * distinct seeded user so methods stay order-independent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MembershipApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private AppUserRepository users;

    private long userId(String externalRef) {
        return users.findByExternalRef(externalRef).orElseThrow().getId();
    }

    private JsonNode subscribe(long userId, String plan, String tier) throws Exception {
        String body = """
                {"userId": %d, "planCode": "%s", "tierCode": "%s"}
                """.formatted(userId, plan, tier);
        MvcResult result = mvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("catalog: plans and tiers are listed and self-describing")
    void catalogIsListed() throws Exception {
        mvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.code=='MONTHLY')].price.amount").exists());

        mvc.perform(get("/api/v1/tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.level=='PLATINUM')].benefits").exists());
    }

    @Test
    @DisplayName("lifecycle: subscribe -> upgrade -> downgrade -> cancel")
    void fullLifecycle() throws Exception {
        long u1 = userId("u1");

        JsonNode created = subscribe(u1, "MONTHLY", "SILVER");
        long subId = created.get("id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(created.get("effectiveTier").asText()).isEqualTo("SILVER");
        assertThat(created.get("pricePaid").get("amount").asDouble()).isEqualTo(199.0);

        // Upgrade to GOLD: selected tier and price rise immediately; an UPGRADE event is recorded.
        mvc.perform(post("/api/v1/subscriptions/{id}/upgrade", subId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetTierCode\":\"GOLD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedTier").value("GOLD"))
                .andExpect(jsonPath("$.pricePaid.amount").value(298.50))
                .andExpect(jsonPath("$.events[?(@.changeType=='UPGRADE')]").exists());

        // Downgrade to SILVER: deferred (pendingTier set, still GOLD until period end).
        mvc.perform(post("/api/v1/subscriptions/{id}/downgrade", subId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetTierCode\":\"SILVER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedTier").value("GOLD"))
                .andExpect(jsonPath("$.pendingTier").value("SILVER"));

        // Cancel: at period end.
        mvc.perform(post("/api/v1/subscriptions/{id}/cancel", subId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(true))
                .andExpect(jsonPath("$.autoRenew").value(false));
    }

    @Test
    @DisplayName("one active subscription per user: a second subscribe is rejected with 409")
    void duplicateActiveSubscriptionRejected() throws Exception {
        long u4 = userId("u4");
        subscribe(u4, "MONTHLY", "SILVER");

        mvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": %d, \"planCode\": \"YEARLY\", \"tierCode\": \"GOLD\"}".formatted(u4)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("earned tier is free: a criteria promotion lifts the effective tier without re-pricing")
    void earnedTierPromotionIsFree() throws Exception {
        long u2 = userId("u2");

        JsonNode created = subscribe(u2, "MONTHLY", "SILVER");
        String priceBefore = created.get("pricePaid").get("amount").asText();
        assertThat(created.get("effectiveTier").asText()).isEqualTo("SILVER");

        // One more order tips u2 over the Gold lifetime-orders threshold.
        mvc.perform(post("/api/v1/demo/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": %d, \"amount\": 500}".formatted(u2)))
                .andExpect(status().isOk());

        // Re-evaluate: effective tier becomes GOLD, selected stays SILVER, price is unchanged.
        MvcResult evaluated = mvc.perform(post("/api/v1/users/{id}/tier/evaluate", u2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveTier").value("GOLD"))
                .andExpect(jsonPath("$.selectedTier").value("SILVER"))
                .andReturn();
        String priceAfter = json.readTree(evaluated.getResponse().getContentAsString())
                .get("pricePaid").get("amount").asText();

        assertThat(priceAfter).isEqualTo(priceBefore);
    }

    @Test
    @DisplayName("benefits preview uses the effective (earned) tier")
    void benefitsPreviewUsesEffectiveTier() throws Exception {
        long u3 = userId("u3"); // prime_metro cohort -> earns PLATINUM for free

        JsonNode created = subscribe(u3, "MONTHLY", "SILVER");
        assertThat(created.get("effectiveTier").asText()).isEqualTo("PLATINUM");
        assertThat(created.get("pricePaid").get("amount").asDouble()).isEqualTo(199.0); // still Silver price

        mvc.perform(post("/api/v1/demo/benefits/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": %d, \"subtotal\": 1000, \"deliveryFee\": 50, \"category\": \"grocery\"}".formatted(u3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PLATINUM"))
                .andExpect(jsonPath("$.deliveryFreed").value(true))
                .andExpect(jsonPath("$.discountOnSubtotal.amount").value(150.00)) // 15% of 1000
                .andExpect(jsonPath("$.finalPayable.amount").value(850.00));
    }

    @Test
    @DisplayName("tier eligibility explains the earned tier and criteria")
    void tierEligibilityExplained() throws Exception {
        long u4 = userId("u4"); // 12 orders / 12000 this month -> PLATINUM by volume
        mvc.perform(get("/api/v1/users/{id}/tier-eligibility", u4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.earnedTier").value("PLATINUM"))
                .andExpect(jsonPath("$.qualifications[?(@.level=='PLATINUM')].qualifies").value(true));
    }
}

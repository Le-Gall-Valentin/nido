package com.nido.api.kitchen.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeEntity;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeIngredientEntity;
import com.nido.api.kitchen.infrastructure.persistence.repository.RecipeIngredientJpaRepository;
import com.nido.api.kitchen.infrastructure.persistence.repository.RecipeJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class MenuControllerIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RecipeJpaRepository recipes;
    @Autowired RecipeIngredientJpaRepository recipeIngredients;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID aliceId;
    private UUID bobId;
    private UUID spaceId;
    private UUID recipeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        members.deleteAll();
        recipes.deleteAll();
        spaces.deleteAll();
        users.deleteAll();

        aliceId = saveUser("alice");
        bobId = saveUser("bob");
        spaceId = saveSharedSpace("Chez Valentin");
        saveMembership(spaceId, aliceId, SpaceRole.OWNER);
        saveMembership(spaceId, bobId, SpaceRole.VIEWER);
        recipeId = saveRecipe(spaceId, "Pâtes bolognaise", 4, "Pâtes", "500", MeasurementUnit.GRAM);
    }

    @Test
    void a_member_can_plan_a_recipe_and_read_it_back_in_range() throws Exception {
        String body = """
            {"date":"2026-09-07","recipeId":"%s","portions":4}
            """.formatted(recipeId);

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/menu")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recipeName").value("Pâtes bolognaise"));

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/menu")
                .param("from", "2026-09-01").param("to", "2026-09-14")
                .cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].portions").value(4));
    }

    @Test
    void a_viewer_cannot_plan_a_meal() throws Exception {
        String body = """
            {"date":"2026-09-07","recipeId":"%s","portions":4}
            """.formatted(recipeId);

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/menu")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void updatePortions_then_remove_a_menu_entry() throws Exception {
        String body = """
            {"date":"2026-09-07","recipeId":"%s","portions":4}
            """.formatted(recipeId);
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/menu")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        String entryId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/spaces/" + spaceId + "/kitchen/menu/" + entryId)
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content("{\"portions\":6}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/kitchen/menu/" + entryId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/menu")
                .param("from", "2026-09-01").param("to", "2026-09-14")
                .cookie(accessTokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shoppingList_scales_ingredients_by_the_entrys_portions() throws Exception {
        String body = """
            {"date":"2026-09-07","recipeId":"%s","portions":2}
            """.formatted(recipeId);
        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/menu")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        // Reference yield is 4 portions at 500g; planned for 2 portions scales to 250g.
        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/menu/shopping-list")
                .param("from", "2026-09-01").param("to", "2026-09-14")
                .cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Pâtes"))
            .andExpect(jsonPath("$[0].quantity").value(250));
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return users.saveAndFlush(user).getId();
    }

    private UUID saveSharedSpace(String name) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName(name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        return spaces.saveAndFlush(space).getId();
    }

    private void saveMembership(UUID spaceId, UUID userId, SpaceRole role) {
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(spaceId);
        member.setUserId(userId);
        member.setRole(role);
        members.saveAndFlush(member);
    }

    private UUID saveRecipe(UUID spaceId, String name, int referencePortions, String ingredientName, String qty, MeasurementUnit unit) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setSpaceId(spaceId);
        recipe.setName(name);
        recipe.setCategory(RecipeCategory.PLAT);
        recipe.setMinutes(35);
        recipe.setReferencePortions(referencePortions);
        recipe.setFavorite(false);
        UUID id = recipes.saveAndFlush(recipe).getId();
        RecipeIngredientEntity ingredient = new RecipeIngredientEntity();
        ingredient.setRecipeId(id);
        ingredient.setPosition(0);
        ingredient.setName(ingredientName);
        ingredient.setQuantity(new BigDecimal(qty));
        ingredient.setUnit(unit);
        recipeIngredients.saveAndFlush(ingredient);
        return id;
    }

    private Cookie accessTokenFor(UUID userId) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", userId + "@test.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}

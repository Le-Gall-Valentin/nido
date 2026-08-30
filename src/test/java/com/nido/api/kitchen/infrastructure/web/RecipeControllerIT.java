package com.nido.api.kitchen.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class RecipeControllerIT {

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
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID aliceId;
    private UUID bobId;
    private UUID spaceId;
    private UUID otherSpaceId;
    private UUID bobsSpaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        members.deleteAll();
        spaces.deleteAll();
        users.deleteAll();

        aliceId = saveUser("alice");
        bobId = saveUser("bob");
        spaceId = saveSharedSpace("Chez Valentin");
        saveMembership(spaceId, aliceId, SpaceRole.OWNER);
        saveMembership(spaceId, bobId, SpaceRole.VIEWER);
        otherSpaceId = saveSharedSpace("Autre groupe");
        saveMembership(otherSpaceId, aliceId, SpaceRole.OWNER);
        bobsSpaceId = saveSharedSpace("Chez Bob");
        saveMembership(bobsSpaceId, bobId, SpaceRole.OWNER);
    }

    private String createRecipeBody() {
        return """
            {"name":"Pâtes bolognaise","description":"Un classique familial.","category":"PLAT","minutes":35,"referencePortions":4,
             "ingredients":[{"name":"Pâtes","quantity":500,"unit":"GRAM"}],"steps":["Cuire les pâtes."],"note":"Encore meilleur réchauffé."}
            """;
    }

    @Test
    void a_member_can_create_and_then_read_the_recipe_back() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pâtes bolognaise"))
            .andExpect(jsonPath("$.description").value("Un classique familial."))
            .andExpect(jsonPath("$.note").value("Encore meilleur réchauffé."))
            .andExpect(jsonPath("$.favorite").value(false));

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/recipes").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].lastPlannedOn").doesNotExist());
    }

    @Test
    void a_recipe_can_be_created_without_a_description_or_note() throws Exception {
        String body = """
            {"name":"Pâtes bolognaise","category":"PLAT","minutes":35,"referencePortions":4,
             "ingredients":[{"name":"Pâtes","quantity":500,"unit":"GRAM"}],"steps":["Cuire les pâtes."]}
            """;

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.note").doesNotExist());
    }

    @Test
    void creating_a_recipe_with_a_too_long_description_is_rejected() throws Exception {
        String body = """
            {"name":"Pâtes bolognaise","description":"%s","category":"PLAT","minutes":35,"referencePortions":4,
             "ingredients":[{"name":"Pâtes","quantity":500,"unit":"GRAM"}],"steps":[]}
            """.formatted("x".repeat(2001));

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_viewer_can_read_but_not_create_a_recipe() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/recipes").cookie(accessTokenFor(bobId)))
            .andExpect(status().isOk());
    }

    @Test
    void creating_a_recipe_without_a_name_is_rejected() throws Exception {
        String body = """
            {"name":"","category":"PLAT","minutes":35,"referencePortions":4,
             "ingredients":[{"name":"Pâtes","quantity":500,"unit":"GRAM"}],"steps":[]}
            """;

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void creating_a_recipe_with_no_ingredients_is_rejected() throws Exception {
        String body = """
            {"name":"Vide","category":"PLAT","minutes":10,"referencePortions":1,
             "ingredients":[],"steps":[]}
            """;

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void creating_a_recipe_with_more_than_100_ingredients_is_rejected() throws Exception {
        String ingredients = IntStream.range(0, 101)
            .mapToObj(i -> "{\"name\":\"Ingrédient " + i + "\",\"quantity\":1,\"unit\":\"GRAM\"}")
            .collect(Collectors.joining(","));
        String body = "{\"name\":\"Trop d'ingrédients\",\"category\":\"PLAT\",\"minutes\":10,\"referencePortions\":1,"
            + "\"ingredients\":[" + ingredients + "],\"steps\":[]}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void creating_a_recipe_with_more_than_100_steps_is_rejected() throws Exception {
        String steps = IntStream.range(0, 101)
            .mapToObj(i -> "\"Étape " + i + "\"")
            .collect(Collectors.joining(","));
        String body = "{\"name\":\"Trop d'étapes\",\"category\":\"PLAT\",\"minutes\":10,\"referencePortions\":1,"
            + "\"ingredients\":[{\"name\":\"Sel\",\"quantity\":1,\"unit\":\"GRAM\"}],\"steps\":[" + steps + "]}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_recipe_from_another_space_is_not_found() throws Exception {
        String location = mockMvc.perform(post("/api/spaces/" + otherSpaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(location).get("id").asText();

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updating_a_recipe_replaces_its_ingredients() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        String updateBody = """
            {"name":"Pâtes bolo maison","category":"PLAT","minutes":40,"referencePortions":4,
             "ingredients":[{"name":"Pâtes","quantity":400,"unit":"GRAM"}],"steps":[]}
            """;

        mockMvc.perform(patch("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId)
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pâtes bolo maison"))
            .andExpect(jsonPath("$.ingredients.length()").value(1));
    }

    @Test
    void toggling_favorite_flips_the_flag() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId + "/favorite")
                .cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void deleting_a_recipe_removes_it() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void copying_a_recipe_creates_it_in_the_destination_and_keeps_the_source() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId + "/copy")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + otherSpaceId + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pâtes bolognaise"))
            .andExpect(jsonPath("$.favorite").value(false));

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/spaces/" + otherSpaceId + "/kitchen/recipes").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void moving_a_recipe_creates_it_in_the_destination_and_removes_the_source() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId + "/move")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + otherSpaceId + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pâtes bolognaise"));

        mockMvc.perform(get("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/spaces/" + otherSpaceId + "/kitchen/recipes").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void a_viewer_can_copy_a_recipe_into_a_space_where_they_have_write_access() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId + "/copy")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void a_viewer_cannot_move_a_recipe_even_into_a_space_where_they_have_write_access() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId + "/move")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void transferring_into_the_same_space_is_rejected() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId + "/copy")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + spaceId + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void copying_into_a_space_the_caller_does_not_belong_to_is_not_found() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(createRecipeBody()))
            .andReturn().getResponse().getContentAsString();
        String recipeId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/kitchen/recipes/" + recipeId + "/copy")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isNotFound());
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

package com.nido.api.space.infrastructure.web;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class SpaceAdminControllerIT {

    // Valeurs exactes de IntegrationTestConfig : tout écart produit un 401 sur tout le fichier.
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
    private UUID adminId;
    private UUID userId;
    private UUID populatedSpaceId;
    private UUID emptySpaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        members.deleteAll();
        spaces.deleteAll();
        users.deleteAll();

        adminId = saveUser("theadmin", Role.ADMIN);
        userId = saveUser("plainuser", Role.USER);

        SpaceEntity populated = new SpaceEntity();
        populated.setType(SpaceType.SHARED);
        populated.setName("Chez Valentin");
        populated.setAccent("#c17a5c");
        populated.setGlyph("🏡");
        populated.setCreatedBy(userId);
        populatedSpaceId = spaces.saveAndFlush(populated).getId();
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(populatedSpaceId);
        member.setUserId(userId);
        member.setRole(SpaceRole.OWNER);
        members.saveAndFlush(member);

        SpaceEntity empty = new SpaceEntity();
        empty.setType(SpaceType.SHARED);
        empty.setName("Groupe orphelin");
        empty.setAccent("#4a7fa0");
        empty.setGlyph("🏠");
        emptySpaceId = spaces.saveAndFlush(empty).getId();
    }

    @Test
    void an_admin_sees_the_metadata_but_never_the_content() throws Exception {
        mockMvc.perform(get("/api/admin/spaces").cookie(accessTokenFor(adminId, Role.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].name").isNotEmpty())
            .andExpect(jsonPath("$.content[0].memberCount").isNumber())
            .andExpect(jsonPath("$.content[0].accent").doesNotExist())
            .andExpect(jsonPath("$.content[0].description").doesNotExist());
    }

    @Test
    void a_plain_user_is_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/spaces").cookie(accessTokenFor(userId, Role.USER)))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleting_a_populated_space_is_refused_and_an_empty_one_is_accepted() throws Exception {
        mockMvc.perform(delete("/api/admin/spaces/" + populatedSpaceId)
                .cookie(accessTokenFor(adminId, Role.ADMIN)))
            .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/admin/spaces/" + emptySpaceId)
                .cookie(accessTokenFor(adminId, Role.ADMIN)))
            .andExpect(status().isNoContent());
    }

    private UUID saveUser(String username, Role role) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(role);
        return users.saveAndFlush(user).getId();
    }

    private Cookie accessTokenFor(UUID id, Role role) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(id.toString())
            .claim("role", role.name())
            .claim("email", id + "@test.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}

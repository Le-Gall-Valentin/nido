package com.nido.api.space.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.infrastructure.persistence.repository.SpaceInvitationJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class SpaceInvitationRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired SpaceInvitationRepositoryAdapter adapter;
    @Autowired SpaceRepositoryAdapter spaceAdapter;
    @Autowired SpaceInvitationJpaRepository spaceInvitationJpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired SpaceMemberJpaRepository spaceMemberJpaRepository;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;
    @Autowired PlatformTransactionManager transactionManager;

    private UUID alice;
    private UUID space;

    @BeforeEach
    void setUp() {
        spaceInvitationJpaRepository.deleteAll();
        spaceMemberJpaRepository.deleteAll();
        spaceJpaRepository.deleteAll();
        userIdentityJpaRepository.deleteAll();
        alice = saveUser("alice");
        space = createSpace(alice).id();
    }

    @Test
    void create_then_findByCode_retrieves_the_invitation_with_every_field() {
        // Postgres stocke TIMESTAMPTZ à la microseconde : on tronque pour comparer à l'identique.
        Instant expiresAt = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.MICROS);

        SpaceInvitation created = adapter.create(space, "camille@exemple.fr", SpaceRole.MEMBER,
            "NIDO-4F9C2A", expiresAt, alice);

        SpaceInvitation found = adapter.findByCode("NIDO-4F9C2A").orElseThrow();
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.spaceId()).isEqualTo(space);
        assertThat(found.email()).isEqualTo("camille@exemple.fr");
        assertThat(found.role()).isEqualTo(SpaceRole.MEMBER);
        assertThat(found.code()).isEqualTo("NIDO-4F9C2A");
        assertThat(found.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(found.expiresAt()).isEqualTo(expiresAt);
        assertThat(found.createdBy()).isEqualTo(alice);
        assertThat(found.acceptedAt()).isNull();
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void a_second_pending_invitation_to_the_same_address_in_the_same_space_is_refused() {
        adapter.create(space, "camille@exemple.fr", SpaceRole.MEMBER, "NIDO-AAAAAA", futureExpiry(), alice);

        assertThatThrownBy(() -> adapter.create(space, "camille@exemple.fr", SpaceRole.VIEWER, "NIDO-BBBBBB", futureExpiry(), alice))
            .isInstanceOf(SpaceException.InvitationAlreadyPending.class);
    }

    @Test
    void after_revoking_the_first_a_new_invitation_for_the_same_address_is_accepted() {
        SpaceInvitation first = adapter.create(space, "camille@exemple.fr", SpaceRole.MEMBER, "NIDO-AAAAAA", futureExpiry(), alice);

        inTransaction(() -> adapter.revoke(first.id()));

        SpaceInvitation second = adapter.create(space, "camille@exemple.fr", SpaceRole.VIEWER, "NIDO-BBBBBB", futureExpiry(), alice);
        assertThat(second.id()).isNotEqualTo(first.id());
    }

    @Test
    void the_same_address_can_be_invited_to_two_different_spaces_at_once() {
        Space otherSpace = createSpace(alice);

        adapter.create(space, "camille@exemple.fr", SpaceRole.MEMBER, "NIDO-AAAAAA", futureExpiry(), alice);
        SpaceInvitation other = adapter.create(otherSpace.id(), "camille@exemple.fr", SpaceRole.MEMBER, "NIDO-BBBBBB", futureExpiry(), alice);

        assertThat(other.spaceId()).isEqualTo(otherSpace.id());
    }

    @Test
    void claim_is_atomic_and_only_succeeds_once() {
        SpaceInvitation invitation = adapter.create(space, "camille@exemple.fr", SpaceRole.MEMBER, "NIDO-AAAAAA", futureExpiry(), alice);
        Instant acceptedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        boolean firstClaim = inTransaction(() -> adapter.claim(invitation.id(), acceptedAt));
        boolean secondClaim = inTransaction(() -> adapter.claim(invitation.id(), acceptedAt));

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();
        SpaceInvitation reloaded = adapter.findById(invitation.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(reloaded.acceptedAt()).isEqualTo(acceptedAt);
    }

    @Test
    void findPendingForEmail_only_returns_pending_unexpired_invitations_for_that_address_case_insensitively() {
        SpaceInvitation valid = adapter.create(space, "camille@exemple.fr", SpaceRole.MEMBER, "NIDO-AAAAAA", futureExpiry(), alice);
        Space otherSpace = createSpace(alice);
        // Même adresse, autre espace : évite la collision avec l'index unique partiel, qui ne
        // porte que sur (space_id, lower(email)).
        SpaceInvitation expired = adapter.create(otherSpace.id(), "camille@exemple.fr", SpaceRole.VIEWER, "NIDO-BBBBBB", Instant.now().minusSeconds(1), alice);
        SpaceInvitation accepted = adapter.create(space, "someoneelse@exemple.fr", SpaceRole.VIEWER, "NIDO-CCCCCC", futureExpiry(), alice);
        inTransaction(() -> adapter.claim(accepted.id(), Instant.now()));

        List<SpaceInvitation> pending = adapter.findPendingForEmail("CAMILLE@Exemple.FR", Instant.now());

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).id()).isEqualTo(valid.id());
        assertThat(pending).noneMatch(i -> i.id().equals(expired.id()));
    }

    @Test
    void deleting_a_space_cascades_to_its_invitations() {
        adapter.create(space, "camille@exemple.fr", SpaceRole.MEMBER, "NIDO-AAAAAA", futureExpiry(), alice);

        spaceAdapter.delete(space);

        assertThat(spaceInvitationJpaRepository.findAll()).isEmpty();
    }

    private static Instant futureExpiry() {
        return Instant.now().plus(Duration.ofDays(7));
    }

    /**
     * `claim` et `revoke` sont des requêtes JPQL de mise à jour : Spring Data exige une
     * transaction active pour les exécuter. En production, elle vient du handler applicatif
     * qui appelle le port ; ce test appelle l'adaptateur directement, il doit donc en ouvrir une.
     */
    private <T> T inTransaction(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private Space createSpace(UUID owner) {
        Space created = spaceAdapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", owner));
        spaceAdapter.add(created.id(), owner, SpaceRole.OWNER);
        return created;
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return userIdentityJpaRepository.saveAndFlush(user).getId();
    }
}

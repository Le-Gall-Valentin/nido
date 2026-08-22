package com.nido.api.space.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceSummaryView;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class SpaceRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired SpaceRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired SpaceMemberJpaRepository spaceMemberJpaRepository;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;

    private UUID alice;
    private UUID bob;

    @BeforeEach
    void setUp() {
        spaceMemberJpaRepository.deleteAll();
        spaceJpaRepository.deleteAll();
        userIdentityJpaRepository.deleteAll();
        alice = saveUser("alice");
        bob = saveUser("bob");
    }

    @Test
    void createPersonal_gives_the_design_appearance_and_an_owner_membership() {
        Space space = adapter.createPersonal(alice);
        adapter.add(space.id(), alice, SpaceRole.OWNER);

        assertThat(space.type()).isEqualTo(SpaceType.PERSONAL);
        assertThat(space.accent()).isEqualTo("#8a7d6b");
        assertThat(space.glyph()).isEqualTo("👤");
        assertThat(space.personalOwnerId()).isEqualTo(alice);
        assertThat(adapter.find(space.id(), alice)).isPresent();
    }

    @Test
    void a_user_cannot_have_two_personal_spaces() {
        adapter.createPersonal(alice);

        assertThatThrownBy(() -> adapter.createPersonal(alice))
            .isInstanceOf(SpaceException.PersonalSpaceAlreadyExists.class);
    }

    @Test
    void a_shared_space_cannot_have_two_owners() {
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);

        assertThatThrownBy(() -> adapter.add(shared.id(), bob, SpaceRole.OWNER))
            .isInstanceOf(SpaceException.OwnerAlreadyExists.class);
    }

    @Test
    void the_same_user_cannot_join_twice() {
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);

        assertThatThrownBy(() -> adapter.add(shared.id(), alice, SpaceRole.MEMBER))
            .isInstanceOf(SpaceException.AlreadyMember.class);
    }

    @Test
    void findMySpaces_returns_the_personal_space_first_with_role_and_member_count() {
        Space personal = adapter.createPersonal(alice);
        adapter.add(personal.id(), alice, SpaceRole.OWNER);
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);
        adapter.add(shared.id(), bob, SpaceRole.MEMBER);

        List<SpaceSummaryView> mine = adapter.findMySpaces(alice);

        assertThat(mine).hasSize(2);
        assertThat(mine.get(0).type()).isEqualTo(SpaceType.PERSONAL);
        assertThat(mine.get(0).memberCount()).isEqualTo(1);
        assertThat(mine.get(1).name()).isEqualTo("Chez Valentin");
        assertThat(mine.get(1).myRole()).isEqualTo(SpaceRole.OWNER);
        assertThat(mine.get(1).memberCount()).isEqualTo(2);
        assertThat(adapter.findMySpaces(bob)).hasSize(1);
    }

    @Test
    void deleting_a_space_cascades_to_its_members() {
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);

        adapter.delete(shared.id());

        assertThat(spaceMemberJpaRepository.findAll()).isEmpty();
        assertThat(adapter.findById(shared.id())).isEmpty();
    }

    @Test
    void findSuccessor_prefers_the_oldest_admin_then_the_oldest_member_and_never_a_viewer() {
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);
        UUID viewer = saveUser("viewer");
        UUID member = saveUser("member");
        adapter.add(shared.id(), viewer, SpaceRole.VIEWER);
        adapter.add(shared.id(), member, SpaceRole.MEMBER);

        assertThat(adapter.findSuccessor(shared.id(), alice))
            .isPresent()
            .get()
            .satisfies(m -> assertThat(m.userId()).isEqualTo(member));

        UUID admin = saveUser("admin");
        adapter.add(shared.id(), admin, SpaceRole.ADMIN);

        assertThat(adapter.findSuccessor(shared.id(), alice))
            .isPresent()
            .get()
            .satisfies(m -> assertThat(m.userId()).isEqualTo(admin));
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return userIdentityJpaRepository.saveAndFlush(user).getId();
    }
}

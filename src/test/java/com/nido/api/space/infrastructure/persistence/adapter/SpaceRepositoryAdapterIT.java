package com.nido.api.space.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceSummaryView;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.model.UpdateSpaceCommand;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class SpaceRepositoryAdapterIT {

    @Autowired SpaceRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired SpaceMemberJpaRepository spaceMemberJpaRepository;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpaceSealers sealers;

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
    void createPersonal_also_generates_a_unique_encryption_salt() {
        Space aliceSpace = adapter.createPersonal(alice);
        Space bobSpace = adapter.createPersonal(bob);

        String aliceSalt = spaceJpaRepository.findById(aliceSpace.id()).orElseThrow().getEncryptionSalt();
        String bobSalt = spaceJpaRepository.findById(bobSpace.id()).orElseThrow().getEncryptionSalt();

        assertThat(aliceSalt).isNotBlank().hasSize(32).matches("[0-9a-f]+");
        assertThat(bobSalt).isNotBlank().hasSize(32).matches("[0-9a-f]+").isNotEqualTo(aliceSalt);
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
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);

        assertThatThrownBy(() -> adapter.add(shared.id(), bob, SpaceRole.OWNER))
            .isInstanceOf(SpaceException.OwnerAlreadyExists.class);
    }

    @Test
    void the_same_user_cannot_join_twice() {
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);

        assertThatThrownBy(() -> adapter.add(shared.id(), alice, SpaceRole.MEMBER))
            .isInstanceOf(SpaceException.AlreadyMember.class);
    }

    @Test
    void findMySpaces_returns_the_personal_space_first_with_role_and_member_count() {
        Space personal = adapter.createPersonal(alice);
        adapter.add(personal.id(), alice, SpaceRole.OWNER);
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));
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
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);

        adapter.delete(shared.id());

        assertThat(spaceMemberJpaRepository.findAll()).isEmpty();
        assertThat(adapter.findById(shared.id())).isEmpty();
    }

    @Test
    void findSuccessor_prefers_the_oldest_admin_then_member_then_viewer() {
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);
        UUID viewer = saveUser("viewer");
        UUID member = saveUser("member");
        adapter.add(shared.id(), viewer, SpaceRole.VIEWER);
        adapter.add(shared.id(), member, SpaceRole.MEMBER);

        // le lecteur est plus ancien, mais le rôle primes sur l'ancienneté
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

    @Test
    void findSuccessor_falls_back_to_a_lone_viewer_rather_than_leaving_nobody() {
        Space shared = adapter.createShared(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));
        adapter.add(shared.id(), alice, SpaceRole.OWNER);
        UUID viewer = saveUser("viewer");
        adapter.add(shared.id(), viewer, SpaceRole.VIEWER);

        // Sans ce repli, le groupe serait supprimé et son contenu détruit sous les yeux
        // des lecteurs restants.
        assertThat(adapter.findSuccessor(shared.id(), alice))
            .isPresent()
            .get()
            .satisfies(m -> assertThat(m.userId()).isEqualTo(viewer));

        assertThat(adapter.findSuccessor(shared.id(), viewer)).isEmpty();
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return userIdentityJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void a_shared_space_keeps_its_name_and_description_encrypted_with_its_own_key() {
        Space space = adapter.createShared(new CreateSharedSpaceCommand("Famille Le Gall 🏡", "Rue des Lilas",
            "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));

        Map<String, Object> row = jdbc.queryForMap("SELECT name_encrypted, description_encrypted FROM spaces WHERE id = ?", space.id());
        SpaceSealer sealer = sealers.forSpace(space.id());
        assertThat((String) row.get("name_encrypted")).startsWith("v2:").doesNotContain("Famille");
        assertThat(sealer.open(SpaceEntity.NAME, space.id(), (String) row.get("name_encrypted"))).isEqualTo("Famille Le Gall 🏡");
        assertThat(sealer.open(SpaceEntity.DESCRIPTION, space.id(), (String) row.get("description_encrypted"))).isEqualTo("Rue des Lilas");
        assertThat(adapter.findById(space.id()).orElseThrow())
            .extracting(Space::name, Space::description).containsExactly("Famille Le Gall 🏡", "Rue des Lilas");
    }

    @Test
    void the_personal_space_name_is_encrypted_too() {
        Space space = adapter.createPersonal(alice);

        String stored = jdbc.queryForObject("SELECT name_encrypted FROM spaces WHERE id = ?", String.class, space.id());

        assertThat(sealers.forSpace(space.id()).open(SpaceEntity.NAME, space.id(), stored)).isEqualTo("Perso");
    }

    @Test
    void renaming_re_encrypts_and_an_empty_description_clears_it() {
        Space space = adapter.createShared(new CreateSharedSpaceCommand("Coloc", "Avant", "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));

        adapter.update(new UpdateSpaceCommand(space.id(), "Coloc rue Y", "", null, null, null));

        Space updated = adapter.findById(space.id()).orElseThrow();
        assertThat(updated.name()).isEqualTo("Coloc rue Y");
        assertThat(updated.description()).isNull();
        assertThat(jdbc.queryForObject("SELECT description_encrypted FROM spaces WHERE id = ?", String.class, space.id())).isNull();
    }

    @Test
    void a_new_description_is_sealed_too() {
        Space space = adapter.createShared(new CreateSharedSpaceCommand("Coloc", null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));

        adapter.update(new UpdateSpaceCommand(space.id(), null, "Rue des Lilas", null, null, null));

        String stored = jdbc.queryForObject("SELECT description_encrypted FROM spaces WHERE id = ?", String.class, space.id());
        assertThat(stored).startsWith("v2:").doesNotContain("Lilas");
        assertThat(sealers.forSpace(space.id()).open(SpaceEntity.DESCRIPTION, space.id(), stored)).isEqualTo("Rue des Lilas");
        assertThat(adapter.findById(space.id()).orElseThrow().description()).isEqualTo("Rue des Lilas");
    }

    @Test
    void my_shared_spaces_come_in_french_alphabetical_order_after_the_personal_one() {
        adapter.add(adapter.createPersonal(alice).id(), alice, SpaceRole.OWNER); // createPersonal adds no membership
        for (String name : List.of("Zeste", "éclair", "abricot")) {
            Space space = adapter.createShared(new CreateSharedSpaceCommand(name, null, "#c17a5c", "🏡", alice, ZoneId.of("Europe/Paris")));
            adapter.add(space.id(), alice, SpaceRole.OWNER);
        }

        assertThat(adapter.findMySpaces(alice)).extracting(SpaceSummaryView::name).containsExactly("Perso", "abricot", "éclair", "Zeste");
    }

    @Test
    void a_name_or_a_description_moved_to_the_other_column_is_refused() {
        Space space = adapter.createShared(new CreateSharedSpaceCommand("Coloc", "Rue des Lilas", "#c17a5c", "🏡", alice,
            ZoneId.of("Europe/Paris")));
        jdbc.update("UPDATE spaces SET name_encrypted = description_encrypted WHERE id = ?", space.id());

        assertThatThrownBy(() -> adapter.findById(space.id())).isInstanceOf(SealedValueRejected.class);
    }
}

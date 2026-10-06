package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class AccountInvitationRepositoryIT {

    @Autowired AccountInvitationRepository invitations;
    @Autowired UserIdentityJpaRepository users;
    @Autowired TransactionTemplate transactions;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = saveUser();
    }

    private UUID saveUser() {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("invited-" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.com");
        user.setRole(Role.USER);
        return users.saveAndFlush(user).getId();
    }

    /** Unique per test: the database is shared, and the hash column is unique. */
    private String hash(char c) {
        return (userId.toString().replace("-", "") + String.valueOf(c).repeat(64)).substring(0, 64 - 1) + c;
    }

    @Test
    void an_invitation_is_found_by_its_hash_and_by_its_account() {
        invitations.save(userId, hash('a'), now, now.plus(Duration.ofDays(7)));

        assertThat(invitations.findByHash(hash('a')))
            .contains(new AccountInvitation(userId, now, now.plus(Duration.ofDays(7))));
        assertThat(invitations.findByUserId(userId)).isPresent();
        assertThat(invitations.findByHash(hash('b'))).isEmpty();
    }

    @Test
    void a_new_invitation_replaces_the_previous_one() {
        invitations.save(userId, hash('a'), now, now.plus(Duration.ofDays(7)));
        invitations.save(userId, hash('b'), now.plusSeconds(60), now.plus(Duration.ofDays(8)));

        assertThat(invitations.findByHash(hash('a'))).isEmpty();
        assertThat(invitations.findByHash(hash('b'))).hasValueSatisfying(invitation ->
            assertThat(invitation.createdAt()).isEqualTo(now.plusSeconds(60)));
    }

    @Test
    void locking_finds_the_invitation_of_the_account_and_nothing_for_one_without() {
        invitations.save(userId, hash('a'), now, now.plus(Duration.ofDays(7)));
        UUID joined = saveUser();

        Optional<AccountInvitation> invited = transactions.execute(status -> invitations.lockForUser(userId));
        Optional<AccountInvitation> none = transactions.execute(status -> invitations.lockForUser(joined));

        assertThat(invited).contains(new AccountInvitation(userId, now, now.plus(Duration.ofDays(7))));
        assertThat(none).isEmpty();
    }

    @Test
    void locking_sees_an_acceptance_committed_after_this_transaction_read_the_row() {
        invitations.save(userId, hash('a'), now, now.plus(Duration.ofDays(7)));
        TransactionTemplate meanwhile = new TransactionTemplate(transactions.getTransactionManager());
        meanwhile.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Optional<AccountInvitation> locked = transactions.execute(status -> {
            assertThat(invitations.findByUserId(userId)).isPresent();
            // The account chooses its password in another transaction, which commits.
            meanwhile.executeWithoutResult(other -> invitations.consumeByHash(hash('a')));
            return invitations.lockForUser(userId);
        });

        assertThat(locked).isEmpty();
    }

    @Test
    void consuming_twice_gives_the_invitation_once() {
        invitations.save(userId, hash('a'), now, now.plus(Duration.ofDays(7)));

        Optional<AccountInvitation> first = transactions.execute(status -> invitations.consumeByHash(hash('a')));
        Optional<AccountInvitation> second = transactions.execute(status -> invitations.consumeByHash(hash('a')));

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(invitations.findByUserId(userId)).isEmpty();
    }

    @Test
    void the_invitations_of_several_accounts_are_read_at_once_and_only_theirs() {
        UUID joined = saveUser();
        invitations.save(userId, hash('a'), now, now.plus(Duration.ofDays(7)));

        assertThat(invitations.findByUserIds(List.of(userId, joined))).containsOnlyKeys(userId);
        assertThat(invitations.findByUserIds(List.of())).isEmpty();
    }

    @Test
    void deleting_an_account_s_invitation_twice_breaks_nothing() {
        invitations.save(userId, hash('a'), now, now.plus(Duration.ofDays(7)));

        transactions.executeWithoutResult(status -> invitations.deleteForUser(userId));
        transactions.executeWithoutResult(status -> invitations.deleteForUser(userId));

        assertThat(invitations.findByUserId(userId)).isEmpty();
    }
}

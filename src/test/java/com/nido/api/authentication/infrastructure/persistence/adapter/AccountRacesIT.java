package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.application.port.in.AcceptAccountInvitationUseCase;
import com.nido.api.authentication.application.port.in.InviteAccountUseCase;
import com.nido.api.authentication.application.port.in.RefreshTokenUseCase;
import com.nido.api.authentication.application.port.in.RequestPasswordResetUseCase;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.RefreshTokenIssuerPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.authentication.infrastructure.persistence.entity.RefreshTokenEntity;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What must not interleave for one account. Each test holds transaction A open at the worst moment,
 * starts B, waits until B is either waiting on a lock or already done, then lets A commit.
 */
@IntegrationTestConfig
class AccountRacesIT {

    @Autowired RefreshTokenUseCase refresh;
    @Autowired RefreshTokenRevocationPort revocation;
    @Autowired RefreshTokenIssuerPort issuer;
    @Autowired UserCredentialsPort credentialsPort;
    @Autowired RequestPasswordResetUseCase requestReset;
    @Autowired InviteAccountUseCase invite;
    @Autowired AcceptAccountInvitationUseCase acceptInvitation;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired PasswordResetTokenJpaRepository resetTokens;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcClient jdbc;

    private UUID userId;
    private String username;

    @BeforeEach
    void setUp() {
        UserIdentityEntity user = new UserIdentityEntity();
        username = "race-" + UUID.randomUUID();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        user.setActive(true);
        userId = users.saveAndFlush(user).getId();
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(userId);
        credential.setPasswordHash("$2a$04$unused.for.these.tests.unused.for.these.tests.unused");
        credentials.save(credential);
    }

    @Test
    void a_session_refreshing_while_every_session_is_revoked_does_not_survive_it() throws Exception {
        UserCredentials creds = credentialsPort.findById(userId).orElseThrow();
        String raw = transactions.execute(status -> issuer.generate(creds, 30));
        CountDownLatch aRotated = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // A: a client refreshing its session, caught between the rotation and its commit.
            Future<?> a = pool.submit(() -> transactions.executeWithoutResult(status -> {
                refresh.refresh(raw);
                aRotated.countDown();
                awaitQuietly(releaseA);
            }));
            assertThat(aRotated.await(10, TimeUnit.SECONDS)).isTrue();

            // B: a reset or a password change ending every session at that very moment.
            Future<?> b = pool.submit(() -> revocation.revokeAllForUser(userId));
            awaitWaitingOrDone(b);
            releaseA.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);

            List<RefreshTokenEntity> tokens = refreshTokens.findAll().stream()
                .filter(token -> token.getUserId().equals(userId)).toList();
            assertThat(tokens).hasSize(2).allMatch(RefreshTokenEntity::isRevoked);
        } finally {
            releaseA.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void two_reset_requests_at_the_same_moment_send_one_link() throws Exception {
        CountDownLatch aIssued = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> transactions.executeWithoutResult(status -> {
                requestReset.request(username);
                aIssued.countDown();
                awaitQuietly(releaseA);
            }));
            assertThat(aIssued.await(10, TimeUnit.SECONDS)).isTrue();

            Future<?> b = pool.submit(() -> requestReset.request(username));
            awaitWaitingOrDone(b);
            releaseA.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);

            assertThat(resetTokens.findAll()).filteredOn(token -> token.getUserId().equals(userId)).hasSize(1);
        } finally {
            releaseA.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void forgot_password_typed_while_the_invitation_is_accepted_waits_then_finds_the_account_joined() throws Exception {
        Invited invited = invitedAccount();
        // Past the pace of reset links, so "forgot password" would renew the invitation.
        jdbc.sql("UPDATE account_invitations SET created_at = created_at - INTERVAL '10 minutes' WHERE user_id = :id")
            .param("id", invited.userId()).update();
        CountDownLatch aAccepted = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // A: the invited person choosing a password, caught between taking the link and its commit.
            Future<?> a = pool.submit(() -> transactions.executeWithoutResult(status -> {
                acceptInvitation.accept(invited.token(), "Welcome-Home-1");
                aAccepted.countDown();
                awaitQuietly(releaseA);
            }));
            assertThat(aAccepted.await(10, TimeUnit.SECONDS)).isTrue();

            // B: "forgot password" typed for that account at that very moment.
            Future<?> b = pool.submit(() -> requestReset.request(invited.username()));
            awaitWaitingOrDone(b);
            releaseA.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);

            assertThat(invitationsOf(invited.userId())).isZero();
            assertThat(credentials.existsById(invited.userId())).isTrue();
        } finally {
            releaseA.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void an_invitation_asked_for_again_while_it_is_accepted_waits_then_finds_the_account_joined() throws Exception {
        Invited invited = invitedAccount();
        CountDownLatch aAccepted = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // A: the invited person choosing a password, caught between taking the link and its commit.
            Future<?> a = pool.submit(() -> transactions.executeWithoutResult(status -> {
                acceptInvitation.accept(invited.token(), "Welcome-Home-1");
                aAccepted.countDown();
                awaitQuietly(releaseA);
            }));
            assertThat(aAccepted.await(10, TimeUnit.SECONDS)).isTrue();

            // B: an administrator sending the invitation again at that very moment.
            Future<Optional<InvitationDelivery>> b = pool.submit(() -> invite.inviteAgain(invited.userId(), "root"));
            awaitWaitingOrDone(b);
            releaseA.countDown();
            a.get(15, TimeUnit.SECONDS);

            assertThat(b.get(15, TimeUnit.SECONDS)).isEmpty();
            assertThat(invitationsOf(invited.userId())).isZero();
        } finally {
            releaseA.countDown();
            pool.shutdownNow();
        }
    }

    private record Invited(UUID userId, String username, String token) {}

    /** An account created by an administrator that has not chosen its password yet. Mail is off: the link comes back. */
    private Invited invitedAccount() {
        UserIdentityEntity user = new UserIdentityEntity();
        String name = "invited-" + UUID.randomUUID();
        user.setUsername(name);
        user.setEmail(name + "@test.com");
        user.setRole(Role.USER);
        user.setActive(true);
        UUID id = users.saveAndFlush(user).getId();
        String url = ((InvitationDelivery.Link) invite.invite(id, "root")).url();
        return new Invited(id, name, url.substring(url.indexOf("token=") + "token=".length()));
    }

    private int invitationsOf(UUID id) {
        return jdbc.sql("SELECT count(*) FROM account_invitations WHERE user_id = :id")
            .param("id", id).query(Integer.class).single();
    }

    /** B either queues behind A's lock — what serialising means — or, without one, runs to the end. */
    private void awaitWaitingOrDone(Future<?> b) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!b.isDone() && waitingLocks() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    private long waitingLocks() {
        return jdbc.sql("SELECT count(*) FROM pg_locks WHERE NOT granted").query(Long.class).single();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

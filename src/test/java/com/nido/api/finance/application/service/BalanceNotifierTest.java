package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.model.SettlementAuthor;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.model.Standing;
import com.nido.api.finance.domain.model.SuggestedTransfer;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.application.port.in.ListSpaceMembersUseCase;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Who hears about the balances of a space: the other side of a repayment; everyone but the author for the opening balance. */
@ExtendWith(MockitoExtension.class)
class BalanceNotifierTest {

    @Mock ListSpaceMembersUseCase members;
    @Mock GetSpaceUseCase spaces;
    @Mock FinanceNotificationPort port;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();
    private final LocalDate day = LocalDate.of(2026, 10, 21);
    private final SpaceMembership aliceActs = new SpaceMembership(UUID.randomUUID(), spaceId, alice, SpaceRole.VIEWER, Instant.now());
    private final Gesture gesture = new Gesture(spaceId, "Chez nous", alice, "alice");
    private final Addressee bobReads = new Addressee(bob, "bob");

    private BalanceNotifier notifier;

    @BeforeEach
    void setUp() {
        lenient().when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice"), member(bob, "bob"), member(carol, "carol")));
        lenient().when(spaces.get(spaceId, aliceActs)).thenReturn(new SpaceDetailView(spaceId, SpaceType.SHARED, "Chez nous",
            null, "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), SpaceRole.VIEWER, 3));
        notifier = new BalanceNotifier(new FinanceAudience(members, spaces), port);
    }

    private static SpaceMemberView member(UUID id, String name) {
        return new SpaceMemberView(id, name, name + "@test.local", SpaceRole.MEMBER, Instant.now());
    }

    private SettlementRecord repayment(UUID from, UUID to) {
        return new SettlementRecord(UUID.randomUUID(), spaceId, from, to, new BigDecimal("50.00"), day);
    }

    @Test
    void a_debtor_paying_back_tells_their_creditor_only() {
        notifier.settlementRecorded(aliceActs, repayment(alice, bob), Optional.empty());

        verify(port).settlementRecorded(gesture, SettlementAuthor.DEBTOR, new BigDecimal("50.00"), day, Standing.EVEN,
            BigDecimal.ZERO, bobReads);
    }

    @Test
    void a_creditor_recording_a_repayment_tells_the_debtor() {
        notifier.settlementRecorded(aliceActs, repayment(bob, alice), Optional.empty());

        verify(port).settlementRecorded(gesture, SettlementAuthor.CREDITOR, new BigDecimal("50.00"), day, Standing.EVEN,
            BigDecimal.ZERO, bobReads);
    }

    @Test
    void what_the_reader_still_owes_is_told_as_theirs_to_pay() {
        notifier.settlementRecorded(aliceActs, repayment(bob, alice),
            Optional.of(new SuggestedTransfer(bob, alice, new BigDecimal("20.00"))));

        verify(port).settlementRecorded(gesture, SettlementAuthor.CREDITOR, new BigDecimal("50.00"), day, Standing.YOU_OWE,
            new BigDecimal("20.00"), bobReads);
    }

    @Test
    void what_the_reader_is_still_owed_is_told_as_owed_to_them() {
        notifier.settlementRecorded(aliceActs, repayment(alice, bob),
            Optional.of(new SuggestedTransfer(alice, bob, new BigDecimal("20.00"))));

        verify(port).settlementRecorded(gesture, SettlementAuthor.DEBTOR, new BigDecimal("50.00"), day, Standing.OWED_TO_YOU,
            new BigDecimal("20.00"), bobReads);
    }

    @Test
    void an_opening_balance_set_tells_everyone_but_the_author() {
        notifier.openingBalanceSet(aliceActs, Optional.empty(), new OpeningBalance(spaceId, new BigDecimal("1520.30"), day));

        verify(port).openingBalanceSet(gesture, new BigDecimal("1520.30"), day, List.of(bobReads, new Addressee(carol, "carol")));
    }

    @Test
    void the_same_opening_balance_again_tells_nobody() {
        notifier.openingBalanceSet(aliceActs, Optional.of(new OpeningBalance(spaceId, new BigDecimal("1520.3"), day)),
            new OpeningBalance(spaceId, new BigDecimal("1520.30"), day));

        verifyNoInteractions(port, members, spaces);
    }

    @Test
    void the_same_amount_on_another_day_is_a_change() {
        notifier.openingBalanceSet(aliceActs, Optional.of(new OpeningBalance(spaceId, new BigDecimal("1520.30"), day)),
            new OpeningBalance(spaceId, new BigDecimal("1520.30"), day.minusDays(1)));

        verify(port).openingBalanceSet(gesture, new BigDecimal("1520.30"), day.minusDays(1),
            List.of(bobReads, new Addressee(carol, "carol")));
    }

    @Test
    void clearing_an_opening_balance_tells_everyone_but_the_author() {
        notifier.openingBalanceCleared(aliceActs, Optional.of(new OpeningBalance(spaceId, new BigDecimal("1520.30"), day)));

        verify(port).openingBalanceCleared(gesture, List.of(bobReads, new Addressee(carol, "carol")));
    }

    @Test
    void clearing_an_absent_opening_balance_tells_nobody() {
        notifier.openingBalanceCleared(aliceActs, Optional.empty());

        verifyNoInteractions(port, members, spaces);
    }
}

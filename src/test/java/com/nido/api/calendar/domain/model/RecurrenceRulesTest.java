package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceRulesTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    @Test
    void aWeeklySeriesNamesItsDaysAndStartsItsWeeksOnMonday() {
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.WEEKLY, 2, Set.of(DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
            null, LocalDate.of(2026, 1, 5), null, true), TORONTO))
            .isEqualTo("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,TH;WKST=MO");
    }

    @Test
    void aDailySeriesSaysItsFrequencyOnly() {
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.DAILY, 1, null, null, LocalDate.of(2026, 1, 5), null, true), TORONTO))
            .isEqualTo("FREQ=DAILY");
    }

    @Test
    void aMonthlyDayIsWrittenSoThatShortMonthsKeepTheirOccurrence() {
        assertThat(monthly(DayRule.SAME_DAY, LocalDate.of(2026, 1, 14))).isEqualTo("FREQ=MONTHLY;BYMONTHDAY=14");
        assertThat(monthly(DayRule.SAME_DAY, LocalDate.of(2026, 1, 29))).isEqualTo("FREQ=MONTHLY;BYMONTHDAY=28,29;BYSETPOS=-1");
        assertThat(monthly(DayRule.SAME_DAY, LocalDate.of(2026, 1, 30))).isEqualTo("FREQ=MONTHLY;BYMONTHDAY=28,29,30;BYSETPOS=-1");
        assertThat(monthly(DayRule.SAME_DAY, LocalDate.of(2026, 1, 31))).isEqualTo("FREQ=MONTHLY;BYMONTHDAY=-1");
        assertThat(monthly(DayRule.LAST_DAY_OF_MONTH, LocalDate.of(2026, 4, 30))).isEqualTo("FREQ=MONTHLY;BYMONTHDAY=-1");
    }

    @Test
    void aMonthlyWeekdayIsWrittenWithItsRank() {
        assertThat(monthly(DayRule.WEEKDAY_OF_MONTH, LocalDate.of(2026, 10, 13))).isEqualTo("FREQ=MONTHLY;BYDAY=2TU");
        assertThat(monthly(DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(2026, 10, 30))).isEqualTo("FREQ=MONTHLY;BYDAY=-1FR");
    }

    @Test
    void aYearlySeriesNamesItsMonth() {
        assertThat(yearly(DayRule.SAME_DAY, LocalDate.of(2026, 10, 14))).isEqualTo("FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=14");
        assertThat(yearly(DayRule.SAME_DAY, LocalDate.of(2028, 2, 29))).isEqualTo("FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=28,29;BYSETPOS=-1");
        assertThat(yearly(DayRule.WEEKDAY_OF_MONTH, LocalDate.of(2026, 11, 26))).isEqualTo("FREQ=YEARLY;BYMONTH=11;BYDAY=4TH");
        assertThat(yearly(DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(2026, 5, 31))).isEqualTo("FREQ=YEARLY;BYMONTH=5;BYDAY=-1SU");
        assertThat(yearly(DayRule.LAST_DAY_OF_MONTH, LocalDate.of(2027, 2, 28))).isEqualTo("FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=-1");
    }

    @Test
    void theLastDayOfAnAllDaySeriesIsADate() {
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.DAILY, 1, null, null, LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 6, 30), true), TORONTO)).isEqualTo("FREQ=DAILY;UNTIL=20260630");
    }

    @Test
    void theLastDayOfATimedSeriesEndsAtMidnightInTheSpacesZoneWrittenInUtc() {
        // 1 March 2026, 23:59:59 in Toronto (UTC-5) is 2 March, 04:59:59 UTC.
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.DAILY, 1, null, null, LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 3, 1), false), TORONTO)).isEqualTo("FREQ=DAILY;UNTIL=20260302T045959Z");
    }

    @Test
    void theLastDayOfATimedSeriesInSummerTimeIsWrittenWithTheSummerOffset() {
        // 1 July 2026, 23:59:59 in Toronto (UTC-4 in summer) is 2 July, 03:59:59 UTC.
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.DAILY, 1, null, null, LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 7, 1), false), TORONTO)).isEqualTo("FREQ=DAILY;UNTIL=20260702T035959Z");
    }

    @Test
    void anIntervalIsWrittenForEveryFrequency() {
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.DAILY, 3, null, null, LocalDate.of(2026, 1, 5), null, true), TORONTO))
            .isEqualTo("FREQ=DAILY;INTERVAL=3");
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.MONTHLY, 2, null, DayRule.WEEKDAY_OF_MONTH,
            LocalDate.of(2026, 10, 13), null, true), TORONTO)).isEqualTo("FREQ=MONTHLY;INTERVAL=2;BYDAY=2TU");
        assertThat(RecurrenceRules.toRrule(series(RecurrenceInterval.YEARLY, 4, null, null, LocalDate.of(2026, 10, 14), null, true), TORONTO))
            .isEqualTo("FREQ=YEARLY;INTERVAL=4;BYMONTH=10;BYMONTHDAY=14");
    }

    @Test
    void severalWeekdaysMakeAWeeklyCandidate() {
        assertThat(RecurrenceRules.candidateFor(recurrence("WEEKLY", 1, List.of(day(0, DayOfWeek.MONDAY), day(0, DayOfWeek.THURSDAY)),
            List.of(), List.of(), List.of()), LocalDate.of(2026, 1, 5)))
            .contains(new RecurrenceRules.Candidate(RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                DayRule.SAME_DAY, LocalDate.of(2026, 1, 5), null));
    }

    @Test
    void everyWorkingDayIsAWeeklySeriesOnFiveDays() {
        List<IcalRecurrence.ByDay> workingDays = List.of(day(0, DayOfWeek.MONDAY), day(0, DayOfWeek.TUESDAY),
            day(0, DayOfWeek.WEDNESDAY), day(0, DayOfWeek.THURSDAY), day(0, DayOfWeek.FRIDAY));
        assertThat(RecurrenceRules.candidateFor(recurrence("DAILY", 1, workingDays, List.of(), List.of(), List.of()),
            LocalDate.of(2026, 1, 5)).map(RecurrenceRules.Candidate::intervalType)).contains(RecurrenceInterval.WEEKLY);
    }

    @Test
    void aRankedWeekdayIsReadIfTheFirstOccurrenceHasThatRank() {
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(day(2, DayOfWeek.TUESDAY)), List.of(), List.of(), List.of()),
            LocalDate.of(2026, 10, 13)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.WEEKDAY_OF_MONTH);
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(day(5, DayOfWeek.TUESDAY)), List.of(), List.of(), List.of()),
            LocalDate.of(2026, 3, 31))).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(day(-1, DayOfWeek.FRIDAY)), List.of(), List.of(), List.of()),
            LocalDate.of(2026, 10, 30)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.LAST_WEEKDAY_OF_MONTH);
    }

    @Test
    void aThirtiethFirstSeenInFebruaryIsAnchoredOnTheLastThirtiethBeforeIt() {
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(), List.of(28, 29, 30), List.of(), List.of(-1)),
            LocalDate.of(2027, 2, 28)))
            .contains(new RecurrenceRules.Candidate(RecurrenceInterval.MONTHLY, 1, null, DayRule.SAME_DAY,
                LocalDate.of(2027, 1, 30), LocalDate.of(2027, 2, 28)));
    }

    @Test
    void aCappedDayLooksBackInTheRhythmOfItsInterval() {
        // Every other month from 28 February 2027: December 2026 is two months before and has a 30th; January is off-rhythm.
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 2, List.of(), List.of(28, 29, 30), List.of(), List.of(-1)),
            LocalDate.of(2027, 2, 28)))
            .contains(new RecurrenceRules.Candidate(RecurrenceInterval.MONTHLY, 2, null, DayRule.SAME_DAY,
                LocalDate.of(2026, 12, 30), LocalDate.of(2027, 2, 28)));
        // Every other year from 28 February 2027: 2025 and 2023 have no leap day, 2021 neither, 2019... none in that rhythm,
        // and none ever will: the rule says the last day of February, every other year.
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 2, List.of(), List.of(28, 29), List.of(2), List.of(-1)),
            LocalDate.of(2027, 2, 28)))
            .contains(new RecurrenceRules.Candidate(RecurrenceInterval.YEARLY, 2, null, DayRule.LAST_DAY_OF_MONTH,
                LocalDate.of(2027, 2, 28), null));
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 3, List.of(), List.of(28, 29), List.of(2), List.of(-1)),
            LocalDate.of(2027, 2, 28)))
            .contains(new RecurrenceRules.Candidate(RecurrenceInterval.YEARLY, 3, null, DayRule.SAME_DAY,
                LocalDate.of(2024, 2, 29), LocalDate.of(2027, 2, 28)));
    }

    @Test
    void theLastDayOfTheMonthIsReadAsSuch() {
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(), List.of(-1), List.of(), List.of()),
            LocalDate.of(2026, 4, 30)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.LAST_DAY_OF_MONTH);
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(), List.of(-1), List.of(), List.of()),
            LocalDate.of(2026, 3, 31)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.SAME_DAY);
    }

    @Test
    void aLeapDayFirstSeenInAnOrdinaryYearIsAnchoredOnTheLastLeapDayBeforeIt() {
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(), List.of(28, 29), List.of(2), List.of(-1)),
            LocalDate.of(2027, 2, 28)))
            .contains(new RecurrenceRules.Candidate(RecurrenceInterval.YEARLY, 1, null, DayRule.SAME_DAY,
                LocalDate.of(2024, 2, 29), LocalDate.of(2027, 2, 28)));
    }

    @Test
    void aPlainDayAShorterMonthWouldSkipHasNoCandidate() {
        // RFC 5545 skips a month without the day, where nido caps it: 29 February every other year is not nido's
        // "same day", which would show 28 February 2026 — nor every fourth year, which parts ways in 2100.
        for (int interval : List.of(1, 2, 4)) {
            assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", interval, List.of(), List.of(), List.of(), List.of()),
                LocalDate.of(2024, 2, 29))).as("every %d years", interval).isEmpty();
            assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", interval, List.of(), List.of(29), List.of(2), List.of()),
                LocalDate.of(2024, 2, 29))).as("every %d years, its day named", interval).isEmpty();
        }
        // Every 24 months from 29 February: always February. Every second month from 31 January: September has no 31st.
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 24, List.of(), List.of(), List.of(), List.of()),
            LocalDate.of(2024, 2, 29))).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 2, List.of(), List.of(31), List.of(), List.of()),
            LocalDate.of(2027, 1, 31))).isEmpty();
    }

    @Test
    void aPlainDayEveryMonthOfTheRuleHasIsACandidate() {
        // Every twelve or twenty-four months from 31 January is always January; every year on 30 April, always April.
        for (int interval : List.of(12, 24)) {
            assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", interval, List.of(), List.of(31), List.of(), List.of()),
                LocalDate.of(2027, 1, 31)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.SAME_DAY);
        }
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(), List.of(), List.of(), List.of()),
            LocalDate.of(2027, 4, 30)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.SAME_DAY);
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(), List.of(28), List.of(), List.of()),
            LocalDate.of(2027, 1, 28)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.SAME_DAY);
    }

    @Test
    void aWeekdayRankedByItsPositionInTheMonthIsARankedWeekday() {
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(day(0, DayOfWeek.TUESDAY)), List.of(), List.of(), List.of(2)),
            LocalDate.of(2026, 10, 13)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.WEEKDAY_OF_MONTH);
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(day(0, DayOfWeek.FRIDAY)), List.of(), List.of(), List.of(-1)),
            LocalDate.of(2026, 10, 30)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.LAST_WEEKDAY_OF_MONTH);
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(day(0, DayOfWeek.THURSDAY)), List.of(), List.of(11), List.of(4)),
            LocalDate.of(2026, 11, 26)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.WEEKDAY_OF_MONTH);
        // Over a whole year, the second Tuesday is January's: no month of nido's.
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(day(0, DayOfWeek.TUESDAY)), List.of(), List.of(), List.of(2)),
            LocalDate.of(2026, 1, 13))).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(day(0, DayOfWeek.TUESDAY)), List.of(), List.of(), List.of(5)),
            LocalDate.of(2026, 3, 31))).isEmpty();
    }

    @Test
    void theLastOfDaysNoMonthOfTheRuleOutlastsIsTheLastDayOfTheMonth() {
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(), List.of(28, 29, 30, 31), List.of(4), List.of(-1)),
            LocalDate.of(2027, 4, 30)))
            .contains(new RecurrenceRules.Candidate(RecurrenceInterval.YEARLY, 1, null, DayRule.LAST_DAY_OF_MONTH,
                LocalDate.of(2027, 4, 30), null));
    }

    @Test
    void aYearlyRankedWeekday() {
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(day(4, DayOfWeek.THURSDAY)), List.of(), List.of(11), List.of()),
            LocalDate.of(2026, 11, 26)).map(RecurrenceRules.Candidate::dayRule)).contains(DayRule.WEEKDAY_OF_MONTH);
    }

    @Test
    void whatNidoCannotSayHasNoCandidate() {
        assertThat(RecurrenceRules.candidateFor(recurrence("HOURLY", 2, List.of(), List.of(), List.of(), List.of()),
            LocalDate.of(2026, 1, 5))).isEmpty();
        assertThat(RecurrenceRules.candidateFor(new IcalRecurrence("DAILY", 1, List.of(), List.of(), List.of(), List.of(), true),
            LocalDate.of(2026, 1, 5))).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(), List.of(1, 15), List.of(), List.of()),
            LocalDate.of(2026, 1, 1))).isEmpty();
    }

    @Test
    void partsAFrequencyCannotTakeHaveNoCandidate() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        assertThat(RecurrenceRules.candidateFor(recurrence("DAILY", 1, List.of(), List.of(5), List.of(), List.of()), monday)).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("DAILY", 2, List.of(day(0, DayOfWeek.MONDAY)), List.of(), List.of(), List.of()),
            monday)).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("WEEKLY", 1, List.of(day(1, DayOfWeek.MONDAY)), List.of(), List.of(), List.of()),
            monday)).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("WEEKLY", 1, List.of(day(0, DayOfWeek.TUESDAY)), List.of(), List.of(), List.of()),
            monday)).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(), List.of(), List.of(1), List.of()), monday)).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(), List.of(), List.of(1, 7), List.of()), monday)).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(), List.of(), List.of(7), List.of()), monday)).isEmpty();
    }

    @Test
    void aRuleWithoutAFrequencyHasNoCandidate() {
        assertThat(RecurrenceRules.candidateFor(new IcalRecurrence(null, 1, List.of(), List.of(), List.of(), List.of(), false),
            LocalDate.of(2026, 1, 5))).isEmpty();
    }

    @Test
    void aHugeIntervalHasNoCandidateAndDoesNotThrow() {
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", Integer.MAX_VALUE, List.of(), List.of(28, 29), List.of(2), List.of(-1)),
            LocalDate.of(2027, 2, 28))).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 100_000_000, List.of(), List.of(28, 29, 30), List.of(), List.of(-1)),
            LocalDate.of(2027, 2, 28))).isEmpty();
    }

    @Test
    void anOutOfRangeDayOrMonthHasNoCandidate() {
        assertThat(RecurrenceRules.candidateFor(recurrence("MONTHLY", 1, List.of(), List.of(28, 2_000_000_000), List.of(), List.of(-1)),
            LocalDate.of(2027, 2, 28))).isEmpty();
        assertThat(RecurrenceRules.candidateFor(recurrence("YEARLY", 1, List.of(), List.of(), List.of(13), List.of()),
            LocalDate.of(2027, 2, 28))).isEmpty();
    }

    private static String monthly(DayRule rule, LocalDate anchor) {
        return RecurrenceRules.toRrule(series(RecurrenceInterval.MONTHLY, 1, null, rule, anchor, null, true), TORONTO);
    }

    private static String yearly(DayRule rule, LocalDate anchor) {
        return RecurrenceRules.toRrule(series(RecurrenceInterval.YEARLY, 1, null, rule, anchor, null, true), TORONTO);
    }

    private static IcalRecurrence recurrence(String frequency, int interval, List<IcalRecurrence.ByDay> byDay,
                                             List<Integer> byMonthDay, List<Integer> byMonth, List<Integer> bySetPos) {
        return new IcalRecurrence(frequency, interval, byDay, byMonthDay, byMonth, bySetPos, false);
    }

    private static IcalRecurrence.ByDay day(int offset, DayOfWeek day) {
        return new IcalRecurrence.ByDay(offset, day);
    }

    static RecurringEventSeries series(RecurrenceInterval interval, int count, Set<DayOfWeek> weekdays, DayRule rule,
                                       LocalDate anchor, LocalDate end, boolean allDay) {
        return new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            allDay, allDay ? null : LocalTime.of(18, 0), allDay ? null : LocalTime.of(19, 0), 0, null,
            interval, count, weekdays, rule, anchor, end, null, List.of(), UUID.randomUUID(), Instant.now());
    }
}

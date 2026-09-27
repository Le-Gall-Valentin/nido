package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A recurring operation of the coming week, already created or still to come. */
public record UpcomingOperation(LocalDate date, String label, BigDecimal amount, Type type, UUID seriesId) {

    /** Spelled as the finance module spells its transaction types; the client reads those names. */
    public enum Type { EXPENSE, INCOME }
}

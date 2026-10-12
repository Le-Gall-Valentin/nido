package com.nido.api.finance.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.UUID;

/** JPQL projection: one month's net, still sealed, with the id of its row, which it is sealed to. */
public record MonthNetRow(UUID id, LocalDate monthStart, String netEncrypted) {}

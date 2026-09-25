package com.nido.api.calendar.domain.model;

/**
 * A fourth copy of this enum, after Tasks, Finance and their schedulers. Deliberate: each
 * bounded context owns its domain vocabulary, and sharing an enum across four of them would
 * make any future divergence — a context needing HOURLY, say — a cross-cutting change.
 */
public enum RecurrenceInterval { DAILY, WEEKLY, MONTHLY, YEARLY }

package com.nido.api.calendar.domain.model;

import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * What reading a file for a space needs to know of that space: which of the file's UIDs it holds already — those events
 * are skipped before anything of them is read —, how many rows the import may write, and how many people take part in
 * each event or series it writes, one row each.
 */
public record ImportTarget(UnaryOperator<Set<String>> alreadyHere, int maxRows, int participantsPerEntry) {}

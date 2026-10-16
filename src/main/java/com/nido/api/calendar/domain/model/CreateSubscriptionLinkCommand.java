package com.nido.api.calendar.domain.model;

import java.util.Set;

public record CreateSubscriptionLinkCommand(String label, Set<CalendarSourceType> sources, boolean onlyMine) {}

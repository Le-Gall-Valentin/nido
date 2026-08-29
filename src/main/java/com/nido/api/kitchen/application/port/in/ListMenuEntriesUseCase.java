package com.nido.api.kitchen.application.port.in;

import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;

public interface ListMenuEntriesUseCase {
    List<MenuEntryView> list(SpaceMembership caller, LocalDate from, LocalDate to);
}

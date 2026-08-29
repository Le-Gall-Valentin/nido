package com.nido.api.kitchen.domain.port.out;

import com.nido.api.kitchen.domain.model.AddMenuEntryCommand;
import com.nido.api.kitchen.domain.model.MenuEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository {
    List<MenuEntry> findBySpaceIdAndDateRange(UUID spaceId, LocalDate from, LocalDate to);
    Optional<MenuEntry> findById(UUID entryId);
    MenuEntry add(AddMenuEntryCommand command);
    void updatePortions(UUID entryId, int portions);
    void remove(UUID entryId);
    Map<UUID, LocalDate> lastPlannedOnBySpace(UUID spaceId);
}

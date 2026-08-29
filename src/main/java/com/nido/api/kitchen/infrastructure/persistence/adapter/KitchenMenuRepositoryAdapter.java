package com.nido.api.kitchen.infrastructure.persistence.adapter;

import com.nido.api.kitchen.domain.model.AddMenuEntryCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.kitchen.infrastructure.persistence.entity.MenuEntryEntity;
import com.nido.api.kitchen.infrastructure.persistence.repository.LastPlannedOn;
import com.nido.api.kitchen.infrastructure.persistence.repository.MenuEntryJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class KitchenMenuRepositoryAdapter implements MenuRepository {

    private final MenuEntryJpaRepository entries;

    public KitchenMenuRepositoryAdapter(MenuEntryJpaRepository entries) {
        this.entries = entries;
    }

    @Override
    public List<MenuEntry> findBySpaceIdAndDateRange(UUID spaceId, LocalDate from, LocalDate to) {
        return entries.findBySpaceIdAndDateBetweenOrderByDateAscPositionAsc(spaceId, from, to).stream()
            .map(KitchenMenuRepositoryAdapter::toDomain)
            .toList();
    }

    @Override
    public Optional<MenuEntry> findById(UUID entryId) {
        return entries.findById(entryId).map(KitchenMenuRepositoryAdapter::toDomain);
    }

    @Override
    public MenuEntry add(AddMenuEntryCommand command) {
        MenuEntryEntity e = new MenuEntryEntity();
        e.setSpaceId(command.spaceId());
        e.setDate(command.date());
        e.setRecipeId(command.recipeId());
        e.setPortions(command.portions());
        e.setPosition((int) entries.countBySpaceIdAndDate(command.spaceId(), command.date()));
        return toDomain(entries.saveAndFlush(e));
    }

    @Override
    public void updatePortions(UUID entryId, int portions) {
        MenuEntryEntity e = entries.findById(entryId).orElseThrow(KitchenException.MenuEntryNotFound::new);
        e.setPortions(portions);
        entries.saveAndFlush(e);
    }

    @Override
    public void remove(UUID entryId) {
        entries.deleteById(entryId);
        entries.flush();
    }

    @Override
    public Map<UUID, LocalDate> lastPlannedOnBySpace(UUID spaceId) {
        return entries.findLastPlannedOnBySpaceId(spaceId).stream()
            .collect(Collectors.toMap(LastPlannedOn::recipeId, LastPlannedOn::lastDate));
    }

    private static MenuEntry toDomain(MenuEntryEntity e) {
        return new MenuEntry(e.getId(), e.getSpaceId(), e.getDate(), e.getRecipeId(), e.getPortions(), e.getPosition());
    }
}

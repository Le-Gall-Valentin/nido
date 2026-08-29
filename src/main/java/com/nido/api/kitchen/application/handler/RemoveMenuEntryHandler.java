package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.RemoveMenuEntryUseCase;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class RemoveMenuEntryHandler implements RemoveMenuEntryUseCase {

    private final MenuRepository menuRepository;

    public RemoveMenuEntryHandler(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Override
    @Transactional
    public void remove(UUID entryId, SpaceMembership caller) {
        MenuEntry existing = menuRepository.findById(entryId).orElseThrow(KitchenException.MenuEntryNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new KitchenException.MenuEntryNotFound();
        }
        caller.ensureCanWrite();
        menuRepository.remove(entryId);
    }
}

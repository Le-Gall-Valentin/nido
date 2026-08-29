package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.UpdateMenuEntryPortionsUseCase;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.UpdateMenuEntryPortionsCommand;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateMenuEntryPortionsHandler implements UpdateMenuEntryPortionsUseCase {

    private final MenuRepository menuRepository;

    public UpdateMenuEntryPortionsHandler(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Override
    @Transactional
    public void updatePortions(UpdateMenuEntryPortionsCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        MenuEntry existing = menuRepository.findById(command.entryId())
            .orElseThrow(KitchenException.MenuEntryNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new KitchenException.MenuEntryNotFound();
        }
        menuRepository.updatePortions(command.entryId(), command.portions());
    }
}

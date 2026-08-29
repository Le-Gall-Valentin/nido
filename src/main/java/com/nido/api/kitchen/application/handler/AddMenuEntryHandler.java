package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.AddMenuEntryUseCase;
import com.nido.api.kitchen.domain.model.AddMenuEntryCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class AddMenuEntryHandler implements AddMenuEntryUseCase {

    private final MenuRepository menuRepository;
    private final RecipeRepository recipeRepository;

    public AddMenuEntryHandler(MenuRepository menuRepository, RecipeRepository recipeRepository) {
        this.menuRepository = menuRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public MenuEntryView add(AddMenuEntryCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        Recipe recipe = recipeRepository.findById(command.recipeId())
            .orElseThrow(KitchenException.RecipeNotFound::new);
        if (!recipe.spaceId().equals(command.spaceId())) {
            throw new KitchenException.RecipeNotFound();
        }
        MenuEntry entry = menuRepository.add(command);
        return new MenuEntryView(entry, recipe);
    }
}

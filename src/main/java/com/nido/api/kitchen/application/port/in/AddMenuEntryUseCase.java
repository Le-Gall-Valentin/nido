package com.nido.api.kitchen.application.port.in;

import com.nido.api.kitchen.domain.model.AddMenuEntryCommand;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.space.domain.model.SpaceMembership;

public interface AddMenuEntryUseCase {
    MenuEntryView add(AddMenuEntryCommand command, SpaceMembership caller);
}

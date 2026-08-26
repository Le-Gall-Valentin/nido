package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceSummaryView;

import java.util.List;
import java.util.UUID;

public interface ListMySpacesUseCase {
    List<SpaceSummaryView> listMine(UUID userId);
}

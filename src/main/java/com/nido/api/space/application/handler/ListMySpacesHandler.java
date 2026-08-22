package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ListMySpacesUseCase;
import com.nido.api.space.domain.model.SpaceSummaryView;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
public class ListMySpacesHandler implements ListMySpacesUseCase {

    private final SpaceRepository spaceRepository;

    public ListMySpacesHandler(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceSummaryView> listMine(UUID userId) {
        return spaceRepository.findMySpaces(userId);
    }
}

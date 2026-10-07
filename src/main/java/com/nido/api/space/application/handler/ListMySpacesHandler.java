package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.NameOrdering;
import com.nido.api.space.application.port.in.ListMySpacesUseCase;
import com.nido.api.space.domain.model.SpaceSummaryView;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
        // Sorted here rather than in SQL, the names being sealed: the personal space first, then the shared ones by name.
        return spaceRepository.findMySpaces(userId).stream()
            .sorted(Comparator.comparing((SpaceSummaryView space) -> space.type() != SpaceType.PERSONAL)
                .thenComparing(SpaceSummaryView::name, NameOrdering.comparator()))
            .toList();
    }
}

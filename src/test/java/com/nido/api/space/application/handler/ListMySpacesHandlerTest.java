package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceSummaryView;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMySpacesHandlerTest {

    @Mock SpaceRepository spaceRepository;

    private ListMySpacesHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ListMySpacesHandler(spaceRepository);
    }

    @Test
    void returns_what_the_repository_gives() {
        UUID userId = UUID.randomUUID();
        SpaceSummaryView personal = new SpaceSummaryView(
            UUID.randomUUID(), SpaceType.PERSONAL, "Perso", "#8a7d6b", "👤", SpaceRole.OWNER, 1);
        when(spaceRepository.findMySpaces(userId)).thenReturn(List.of(personal));

        List<SpaceSummaryView> result = handler.listMine(userId);

        assertThat(result).containsExactly(personal);
    }
}

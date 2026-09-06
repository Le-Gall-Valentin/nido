package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCategoriesHandlerTest {

    @Mock CategoryRepository categoryRepository;
    private ListCategoriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListCategoriesHandler(categoryRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void seeds_defaults_before_listing_when_the_space_has_no_categories_yet() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(false);
        Category seeded = new Category(UUID.randomUUID(), spaceId, "Alimentation", "#f59e0b", "Utensils", true, TransactionType.EXPENSE);
        when(categoryRepository.findBySpaceId(spaceId)).thenReturn(List.of(seeded));

        List<Category> result = handler.list(membership());

        verify(categoryRepository, org.mockito.Mockito.times(8)).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true));
        assertThat(result).containsExactly(seeded);
    }

    @Test
    void takes_the_seeding_lock_before_checking_whether_defaults_already_exist() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(false);
        when(categoryRepository.findBySpaceId(spaceId)).thenReturn(List.of());

        handler.list(membership());

        verify(categoryRepository).lockForSeeding(spaceId);
    }

    @Test
    void does_not_reseed_when_the_space_already_has_categories() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(true);
        when(categoryRepository.findBySpaceId(spaceId)).thenReturn(List.of());

        handler.list(membership());

        verify(categoryRepository, org.mockito.Mockito.never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}

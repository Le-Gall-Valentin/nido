package com.nido.api.shopping.application.handler;

import com.nido.api.shared.model.MeasurementUnit;
import com.nido.api.shopping.domain.model.ImportShoppingItemsCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingImportLine;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportShoppingItemsFromMenuHandlerTest {

    @Mock ShoppingItemRepository itemRepository;
    @Mock ShoppingCategoryRepository categoryRepository;
    private ImportShoppingItemsFromMenuHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ImportShoppingItemsFromMenuHandler(itemRepository, categoryRepository);
        lenient().when(categoryRepository.findBySpaceId(spaceId))
            .thenReturn(List.of(new ShoppingCategory(categoryId, spaceId, "Épicerie", 0, false)));
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_line_with_no_matching_pending_item_is_added_as_new() {
        when(itemRepository.findBySpaceIdAndDoneFalse(spaceId)).thenReturn(List.of());
        ShoppingImportLine line = new ShoppingImportLine("Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, categoryId);
        ShoppingItem created = new ShoppingItem(UUID.randomUUID(), spaceId, categoryId, "Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, false, 0);
        when(itemRepository.add(any())).thenReturn(created);

        List<ShoppingItem> result = handler.importItems(
            new ImportShoppingItemsCommand(spaceId, List.of(line)), membership(SpaceRole.MEMBER));

        verify(itemRepository).add(argThat(cmd -> cmd.name().equals("Poulet") && cmd.categoryId().equals(categoryId)));
        verify(itemRepository, never()).update(any());
        assertThat(result).containsExactly(created);
    }

    @Test
    void a_line_matching_an_existing_pending_item_by_normalized_name_updates_it_instead_of_duplicating() {
        UUID existingId = UUID.randomUUID();
        ShoppingItem existing = new ShoppingItem(existingId, spaceId, categoryId, "Poulets", new BigDecimal("800"), MeasurementUnit.GRAM, false, 0);
        when(itemRepository.findBySpaceIdAndDoneFalse(spaceId)).thenReturn(List.of(existing));
        ShoppingImportLine line = new ShoppingImportLine("Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, categoryId);
        ShoppingItem updated = new ShoppingItem(existingId, spaceId, categoryId, "Poulets", new BigDecimal("1"), MeasurementUnit.KILOGRAM, false, 0);
        when(itemRepository.update(new UpdateShoppingItemCommand(existingId, spaceId, categoryId, "Poulets", new BigDecimal("1"), MeasurementUnit.KILOGRAM)))
            .thenReturn(updated);

        List<ShoppingItem> result = handler.importItems(
            new ImportShoppingItemsCommand(spaceId, List.of(line)), membership(SpaceRole.MEMBER));

        verify(itemRepository, never()).add(any());
        assertThat(result).containsExactly(updated);
    }

    @Test
    void a_done_item_with_the_same_name_is_never_matched_a_fresh_line_is_added_instead() {
        ShoppingItem done = new ShoppingItem(UUID.randomUUID(), spaceId, categoryId, "Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, true, 0);
        // findBySpaceIdAndDoneFalse excludes done items by construction — the done item never appears here.
        when(itemRepository.findBySpaceIdAndDoneFalse(spaceId)).thenReturn(List.of());
        ShoppingImportLine line = new ShoppingImportLine("Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, categoryId);
        ShoppingItem created = new ShoppingItem(UUID.randomUUID(), spaceId, categoryId, "Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, false, 0);
        when(itemRepository.add(any())).thenReturn(created);

        List<ShoppingItem> result = handler.importItems(
            new ImportShoppingItemsCommand(spaceId, List.of(line)), membership(SpaceRole.MEMBER));

        assertThat(result).containsExactly(created);
        assertThat(done.done()).isTrue(); // sanity: the fixture really is done, and was still not reused
    }

    @Test
    void reimporting_an_unchanged_line_is_idempotent_it_updates_the_same_item_not_a_new_one() {
        UUID existingId = UUID.randomUUID();
        ShoppingItem existing = new ShoppingItem(existingId, spaceId, categoryId, "Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, false, 0);
        when(itemRepository.findBySpaceIdAndDoneFalse(spaceId)).thenReturn(List.of(existing));
        ShoppingImportLine line = new ShoppingImportLine("Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, categoryId);
        when(itemRepository.update(new UpdateShoppingItemCommand(existingId, spaceId, categoryId, "Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM)))
            .thenReturn(existing);

        handler.importItems(new ImportShoppingItemsCommand(spaceId, List.of(line)), membership(SpaceRole.MEMBER));

        verify(itemRepository, never()).add(any());
        verify(itemRepository).update(new UpdateShoppingItemCommand(existingId, spaceId, categoryId, "Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM));
    }

    @Test
    void a_category_from_another_space_is_rejected() {
        UUID foreignCategoryId = UUID.randomUUID();
        when(itemRepository.findBySpaceIdAndDoneFalse(spaceId)).thenReturn(List.of());
        ShoppingImportLine line = new ShoppingImportLine("Poulet", new BigDecimal("1"), MeasurementUnit.KILOGRAM, foreignCategoryId);

        assertThatThrownBy(() -> handler.importItems(
            new ImportShoppingItemsCommand(spaceId, List.of(line)), membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.CategoryNotFound.class);
    }

    @Test
    void a_viewer_cannot_import() {
        assertThatThrownBy(() -> handler.importItems(
            new ImportShoppingItemsCommand(spaceId, List.of()), membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}

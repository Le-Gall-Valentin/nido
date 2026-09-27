package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.ShoppingCard;
import com.nido.api.dashboard.domain.model.ShoppingGroup;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.shopping.application.port.in.ListShoppingCategoriesUseCase;
import com.nido.api.shopping.application.port.in.ListShoppingItemsUseCase;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShoppingDashboardSourceTest {

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    private final ListShoppingItemsUseCase listItems = mock(ListShoppingItemsUseCase.class);
    private final ListShoppingCategoriesUseCase listCategories = mock(ListShoppingCategoriesUseCase.class);

    private final ShoppingCategory fruits = new ShoppingCategory(UUID.randomUUID(), spaceId, "Fruits et légumes", 0, false);
    private final ShoppingCategory grocery = new ShoppingCategory(UUID.randomUUID(), spaceId, "Épicerie", 1, false);
    private final ShoppingCategory other = new ShoppingCategory(UUID.randomUUID(), spaceId, "Autre", 9, true);

    @Test
    void groupsFollowTheCategoryOrderWithTheFirstThreeNamesInListOrder() {
        // Checks items and categories reach ShoppingAisles whole; its rules are in ShoppingAislesTest.
        when(listItems.list(caller)).thenReturn(List.of(
            item(grocery, "Pâtes", 4, false),
            item(fruits, "Pommes", 3, false),
            item(fruits, "Tomates", 0, false),
            item(fruits, "Courgettes", 1, false),
            item(fruits, "Poires", 2, false)));
        when(listCategories.list(caller)).thenReturn(List.of(other, grocery, fruits));

        ShoppingCard card = (ShoppingCard) read().card();

        assertThat(card.remaining()).isEqualTo(5);
        assertThat(card.categories()).containsExactly(
            new ShoppingGroup(fruits.id(), "Fruits et légumes", 4, List.of("Tomates", "Courgettes", "Poires")),
            new ShoppingGroup(grocery.id(), "Épicerie", 1, List.of("Pâtes")));
    }

    @Test
    void nothingLeftToBuyMeansNoCardAndNoCategoryRead() {
        when(listItems.list(caller)).thenReturn(List.of(item(fruits, "Tomates", 0, true)));

        SourceResult result = read();

        assertThat(result.card()).isNull();
        verifyNoInteractions(listCategories);
    }

    @Test
    void itIsTheShoppingSource() {
        assertThat(new ShoppingDashboardSource(listItems, listCategories).kind()).isEqualTo(CardKind.SHOPPING);
    }

    private SourceResult read() {
        return new ShoppingDashboardSource(listItems, listCategories)
            .read(new DashboardContext(caller, "me@test.com", LocalDate.of(2026, 9, 26), SpaceType.SHARED));
    }

    private ShoppingItem item(ShoppingCategory category, String name, int position, boolean done) {
        return new ShoppingItem(UUID.randomUUID(), spaceId, category.id(), name, null, null, done, position);
    }
}

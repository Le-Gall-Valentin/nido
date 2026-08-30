package com.nido.api.kitchen.infrastructure.web;

import com.nido.api.kitchen.domain.model.KitchenException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class KitchenExceptionHandlerTest {

    private final KitchenExceptionHandler handler = new KitchenExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/spaces/x/kitchen/recipes/y");

    @Test
    void recipe_not_found_is_a_404() {
        ResponseEntity<ProblemDetail> response = handler.handle(new KitchenException.RecipeNotFound(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void menu_entry_not_found_is_a_404() {
        ResponseEntity<ProblemDetail> response = handler.handle(new KitchenException.MenuEntryNotFound(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void same_space_transfer_is_a_400() {
        ResponseEntity<ProblemDetail> response = handler.handle(new KitchenException.SameSpaceTransfer(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}

package com.nido.api.space.infrastructure.web;

import com.nido.api.space.domain.model.SpaceException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceExceptionHandlerTest {

    private final SpaceExceptionHandler handler = new SpaceExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/spaces/x");

    @Test
    void not_a_member_is_a_404_so_the_space_existence_stays_hidden() {
        ResponseEntity<ProblemDetail> response = handler.handle(new SpaceException.NotAMember(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("NotAMember");
    }

    @Test
    void insufficient_role_is_a_403() {
        assertThat(handler.handle(new SpaceException.InsufficientRole(), request)
            .getStatusCode().value()).isEqualTo(403);
        assertThat(handler.handle(new SpaceException.OwnerRequired(), request)
            .getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void business_rule_violations_are_409_or_422() {
        assertThat(handler.handle(new SpaceException.LastOwnerCannotLeave(), request)
            .getStatusCode().value()).isEqualTo(409);
        assertThat(handler.handle(new SpaceException.SelfManagementForbidden(), request)
            .getStatusCode().value()).isEqualTo(409);
        assertThat(handler.handle(new SpaceException.PersonalSpaceImmutable(), request)
            .getStatusCode().value()).isEqualTo(422);
        assertThat(handler.handle(new SpaceException.InvalidAppearance(), request)
            .getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void data_integrity_error_is_a_500() {
        assertThat(handler.handle(new SpaceException.DataIntegrityError(), request)
            .getStatusCode().value()).isEqualTo(500);
    }
}

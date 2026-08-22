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
    void not_a_member_is_indistinguishable_from_an_unknown_space() {
        ResponseEntity<ProblemDetail> notAMember = handler.handle(new SpaceException.NotAMember(), request);
        ResponseEntity<ProblemDetail> notFound = handler.handle(new SpaceException.SpaceNotFound(), request);

        assertThat(notAMember.getStatusCode().value()).isEqualTo(404);
        assertThat(notAMember.getBody()).isNotNull();
        assertThat(notFound.getBody()).isNotNull();
        // Aucun champ de la réponse ne doit trahir qu'un contexte existe bel et bien.
        assertThat(notAMember.getBody().getTitle()).isEqualTo(notFound.getBody().getTitle());
        assertThat(notAMember.getBody().getDetail()).isEqualTo(notFound.getBody().getDetail());
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

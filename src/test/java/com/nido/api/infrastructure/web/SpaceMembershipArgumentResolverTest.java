package com.nido.api.infrastructure.web;

import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

/**
 * The role floor that {@code @CurrentMembership(min = ...)} declares is enforced here and nowhere
 * else, and fifty routes now depend on it — yet it had no test at all. These cover the mechanism
 * itself; {@link com.nido.api.WebAuthorizationConventionsTest} covers that the routes declare it.
 */
@ExtendWith(MockitoExtension.class)
class SpaceMembershipArgumentResolverTest {

    @Mock ResolveMembershipUseCase resolveMembershipUseCase;
    private SpaceMembershipArgumentResolver resolver;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    /** Source of the {@link MethodParameter}s below — the shape a real route declares. */
    @SuppressWarnings("unused")
    static class Routes {
        void read(@CurrentMembership SpaceMembership membership) {}

        void write(@CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {}

        void manage(@CurrentMembership(min = SpaceRole.ADMIN) SpaceMembership membership) {}

        void unannotated(SpaceMembership membership) {}

        void wrongType(@CurrentMembership String notAMembership) {}
    }

    private static MethodParameter parameterOf(String methodName, Class<?> parameterType) {
        try {
            return new MethodParameter(Routes.class.getDeclaredMethod(methodName, parameterType), 0);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void setUp() {
        resolver = new SpaceMembershipArgumentResolver(resolveMembershipUseCase);
        authenticateAs(userId.toString());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String principalName) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principalName, "n/a", List.of()));
    }

    private NativeWebRequest requestFor(String rawSpaceId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (rawSpaceId != null) {
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("spaceId", rawSpaceId));
        }
        return new ServletWebRequest(request);
    }

    private void callerIs(SpaceRole role) {
        lenient().when(resolveMembershipUseCase.resolve(spaceId, userId)).thenReturn(
            new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now()));
    }

    private Object resolve(String methodName) {
        return resolver.resolveArgument(parameterOf(methodName, SpaceMembership.class), null,
            requestFor(spaceId.toString()), null);
    }

    @Test
    void resolves_only_a_membership_parameter_that_carries_the_annotation() {
        assertThat(resolver.supportsParameter(parameterOf("read", SpaceMembership.class))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("unannotated", SpaceMembership.class))).isFalse();
        assertThat(resolver.supportsParameter(parameterOf("wrongType", String.class))).isFalse();
    }

    @Test
    void a_viewer_reaches_a_route_that_declares_no_floor() {
        callerIs(SpaceRole.VIEWER);

        assertThat(resolve("read")).isInstanceOf(SpaceMembership.class);
    }

    @Test
    void a_viewer_is_refused_by_a_route_that_requires_a_member() {
        // The whole point of the floor: refused here, before the handler is entered at all.
        callerIs(SpaceRole.VIEWER);

        assertThatThrownBy(() -> resolve("write")).isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_member_is_refused_by_a_route_that_requires_an_admin() {
        callerIs(SpaceRole.MEMBER);

        assertThatThrownBy(() -> resolve("manage")).isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_role_above_the_floor_passes_it() {
        // The floor is a minimum, not an equality — an OWNER must reach a MEMBER route.
        callerIs(SpaceRole.OWNER);

        assertThat(resolve("write")).isInstanceOf(SpaceMembership.class);
    }

    @Test
    void a_malformed_space_id_says_only_that_the_caller_is_not_a_member() {
        // Never a 400 mentioning the id: a malformed path must not read differently from an
        // unknown space, or it tells the caller which spaces exist.
        assertThatThrownBy(() -> resolver.resolveArgument(
            parameterOf("read", SpaceMembership.class), null, requestFor("not-a-uuid"), null))
            .isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void an_unauthenticated_caller_is_not_a_member() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> resolve("read")).isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void a_principal_that_is_not_a_user_id_is_not_a_member() {
        authenticateAs("anonymousUser");

        assertThatThrownBy(() -> resolve("read")).isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void a_route_without_a_space_id_is_a_programming_error_not_a_client_error() {
        assertThatThrownBy(() -> resolver.resolveArgument(
            parameterOf("read", SpaceMembership.class), null, requestFor(null), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("spaceId");
    }
}

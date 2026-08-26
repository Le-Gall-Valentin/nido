package com.nido.api.infrastructure.web;

import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

// Les routes de contexte déclarent aussi un @PathVariable UUID spaceId, pour la
// lisibilité et la documentation OpenAPI. Spring convertit ce paramètre avant ce
// résolveur, donc un identifiant syntaxiquement invalide produit un 400 : choix
// assumé, une chaîne malformée ne désigne aucun contexte. Le parsing ci-dessous
// reste défensif, pour une éventuelle route qui ne déclarerait pas la variable.
@Component
public class SpaceMembershipArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String SPACE_ID = "spaceId";

    private final ResolveMembershipUseCase resolveMembershipUseCase;

    public SpaceMembershipArgumentResolver(ResolveMembershipUseCase resolveMembershipUseCase) {
        this.resolveMembershipUseCase = resolveMembershipUseCase;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMembership.class)
            && SpaceMembership.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Map<String, String> pathVariables = (Map<String, String>)
            webRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        String rawSpaceId = pathVariables == null ? null : pathVariables.get(SPACE_ID);
        if (rawSpaceId == null) {
            throw new IllegalStateException(
                "@CurrentMembership requires a {spaceId} path variable on the handler route");
        }
        UUID spaceId;
        try {
            spaceId = UUID.fromString(rawSpaceId);
        } catch (IllegalArgumentException e) {
            // Un identifiant mal formé ne doit pas révéler autre chose qu'un 404.
            throw new SpaceException.NotAMember();
        }
        SpaceMembership membership = resolveMembershipUseCase.resolve(spaceId, currentUserId());
        // Plancher déclaratif : la route dit le rôle qu'elle exige, le résolveur le tient.
        // Sans cela chaque futur handler scopé devrait penser à appeler ensureCanWrite().
        CurrentMembership annotation = parameter.getParameterAnnotation(CurrentMembership.class);
        SpaceRole required = annotation == null ? SpaceRole.VIEWER : annotation.min();
        if (!membership.role().atLeast(required)) {
            throw new SpaceException.InsufficientRole();
        }
        return membership;
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SpaceException.NotAMember();
        }
        // Le nom du principal est l'UUID du compte (cf. CustomUserDetails.getUsername()).
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new SpaceException.NotAMember();
        }
    }
}

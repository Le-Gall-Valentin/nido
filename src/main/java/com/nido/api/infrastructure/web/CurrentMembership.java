package com.nido.api.infrastructure.web;

import com.nido.api.space.domain.model.SpaceRole;

import java.lang.annotation.*;

/**
 * Injecte le SpaceMembership de l'appelant dans le contexte {spaceId} de la route.
 * Volontairement hors du bounded context `space` : les futures features scopées
 * s'en servent sans dépendre de space.infrastructure.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentMembership {

    /**
     * Rôle minimal exigé dans le contexte. VIEWER par défaut, soit aucune exigence :
     * une route qui écrit doit déclarer explicitement MEMBER ou davantage.
     */
    SpaceRole min() default SpaceRole.VIEWER;
}

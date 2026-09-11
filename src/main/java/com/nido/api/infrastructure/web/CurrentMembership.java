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
     *
     * <p>Cette règle n'est pas qu'une convention : {@code WebAuthorizationConventionsTest} fait
     * échouer le build pour toute route d'écriture qui ne déclare pas son plancher, et
     * {@code SpaceMembershipArgumentResolverTest} couvre le fait que le plancher est réellement
     * appliqué. Le plancher filtre au bord et ne remplace pas les gardes du handler : il ne connaît
     * que le rôle de l'appelant, là où un handler vérifie aussi le contexte et la ressource.
     */
    SpaceRole min() default SpaceRole.VIEWER;
}

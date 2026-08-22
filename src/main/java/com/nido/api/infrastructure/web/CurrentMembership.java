package com.nido.api.infrastructure.web;

import java.lang.annotation.*;

/**
 * Injecte le SpaceMembership de l'appelant dans le contexte {spaceId} de la route.
 * Volontairement hors du bounded context `space` : les futures features scopées
 * s'en servent sans dépendre de space.infrastructure.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentMembership {}

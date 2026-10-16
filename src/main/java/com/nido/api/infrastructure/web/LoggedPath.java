package com.nido.api.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/** A request's path as it may be logged: the token of a subscription address is its whole credential, and never is. */
public final class LoggedPath {

    private static final Pattern SUBSCRIPTION = Pattern.compile("^/ical/[^/]+");

    private LoggedPath() {}

    public static String of(HttpServletRequest request) {
        return SUBSCRIPTION.matcher(request.getRequestURI()).replaceFirst("/ical/***.ics");
    }
}

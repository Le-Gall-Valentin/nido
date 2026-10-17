package com.nido.api.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * A request's path as it may be logged: the token of a subscription address is its whole credential, and never is.
 * The decision is made on the path the router reads (decoded, without path parameters), not on the raw text, so no
 * spelling of the address escapes it; a subscription address is never logged with any part of its raw form.
 */
public final class LoggedPath {

    private static final Pattern SUBSCRIPTION = Pattern.compile("^/ical/[^/]+.*");
    private static final Pattern PATH_PARAMETERS = Pattern.compile(";[^/]*");
    private static final Pattern SLASHES = Pattern.compile("/{2,}");
    private static final String MASKED = "/ical/***.ics";

    private LoggedPath() {}

    public static String of(HttpServletRequest request) {
        String raw = request.getRequestURI();
        String routed;
        try {
            routed = SLASHES.matcher(URLDecoder.decode(PATH_PARAMETERS.matcher(raw).replaceAll("").replace("+", "%2B"),
                StandardCharsets.UTF_8)).replaceAll("/");
        } catch (IllegalArgumentException e) {
            // Not a path the router can read either: say nothing of it rather than risk a token.
            return "/***";
        }
        return SUBSCRIPTION.matcher(routed).matches() ? MASKED : raw;
    }
}

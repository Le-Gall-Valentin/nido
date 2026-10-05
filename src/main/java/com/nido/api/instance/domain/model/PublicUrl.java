package com.nido.api.instance.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

/**
 * The address people reach Nido at: the links in mails start with it, and whether it is https decides
 * the cookies' Secure flag. Plain http is accepted for any host — on a home network the whole app is
 * http anyway, and refusing would only stop the installation; the pages warn instead. Kept without a
 * trailing slash, scheme and host in lower case. No path: Nido answers at the root of its host only —
 * its pages, /api and the cookies' paths all start at / — so the links of an address with a path would
 * lead nowhere.
 */
public record PublicUrl(String value) {

    public static Optional<PublicUrl> parse(String typed) {
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(typed.strip());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return Optional.empty();
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        if (!path.isEmpty() && !path.equals("/")) {
            return Optional.empty();
        }
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        return Optional.of(new PublicUrl(scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + port));
    }

    public boolean secure() {
        return value.startsWith("https://");
    }
}

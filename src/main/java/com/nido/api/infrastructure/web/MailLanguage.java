package com.nido.api.infrastructure.web;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The language a mail is written in: the account holder's own when their account has one; otherwise
 * the language the current request asks for — the frontend sends the language on screen as
 * {@code Accept-Language} — and otherwise English, the app's own fallback.
 *
 * <p>The account comes first on purpose: "forgot password" can be asked by anyone for any account,
 * and the mail is for the holder, not for whoever filled the form.
 */
public final class MailLanguage {

    private static final List<String> SUPPORTED = List.of("fr", "en");

    private MailLanguage() {}

    public static Locale resolve(String accountLanguage) {
        if (accountLanguage != null && SUPPORTED.contains(accountLanguage)) {
            return Locale.of(accountLanguage);
        }
        return ofCurrentRequest().orElse(Locale.ENGLISH);
    }

    static Optional<Locale> ofCurrentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        String header = attributes.getRequest().getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        try {
            String tag = Locale.lookupTag(Locale.LanguageRange.parse(header), SUPPORTED);
            return Optional.ofNullable(tag).map(Locale::forLanguageTag);
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }
}

package com.nido.api.infrastructure.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Refuses, as a request body is read, a text holding half of a character: a UTF-16 surrogate without its pair, which
 * JSON can spell ({@code "\ud83e"}) but no text holds. Such a text could be neither encrypted — the encryption encodes
 * it in UTF-8, which refuses it — nor stored as typed, the database driver writing {@code ?} in its place: a 400 here,
 * for every text of every body, rather than a 500 deep down.
 */
@Configuration(proxyBeanMethods = false)
public class WellFormedTextConfig {

    @Bean
    JacksonModule wellFormedText() {
        return new SimpleModule("well-formed-text").addDeserializer(String.class, new WellFormedStringDeserializer());
    }

    static boolean holdsHalfACharacter(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                i++;
            } else if (Character.isSurrogate(c)) {
                return true;
            }
        }
        return false;
    }

    /** Not marked as Jackson's own, so collections of texts go through it too instead of their shortcut. */
    static final class WellFormedStringDeserializer extends StringDeserializer {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) {
            String text = super.deserialize(parser, context);
            if (text != null && holdsHalfACharacter(text)) {
                return context.reportInputMismatch(this, "A text holds half of a character (a lone UTF-16 surrogate)");
            }
            return text;
        }
    }
}

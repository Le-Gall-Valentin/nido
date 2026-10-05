package com.nido.api.instance.domain.model;

/** An encryption key the installation gave, and where from — for the messages. Never shows the key. */
public record ProvidedKey(String value, String origin) {
    @Override
    public String toString() {
        return "ProvidedKey[origin=" + origin + ", value=***]";
    }
}

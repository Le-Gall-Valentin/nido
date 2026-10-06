package com.nido.api.infrastructure.config;

import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** The usual backfill: a module's tables, each value encrypted with the key of its row's space. */
public final class PlaintextTablesBackfill implements EncryptionBackfill {

    private final PlaintextTableEncryptor encryptor;
    private final Function<UUID, TextEncryptor> keyOfSpace;
    private final List<PlaintextTable> tables;

    public PlaintextTablesBackfill(PlaintextTableEncryptor encryptor, Function<UUID, TextEncryptor> keyOfSpace,
                                   PlaintextTable... tables) {
        this.encryptor = encryptor;
        this.keyOfSpace = keyOfSpace;
        this.tables = List.of(tables);
    }

    @Override
    public boolean pending() {
        return tables.stream().anyMatch(encryptor::pending);
    }

    @Override
    public Map<String, Integer> run() {
        Map<String, Integer> rewritten = new LinkedHashMap<>();
        for (PlaintextTable table : tables) {
            rewritten.put(table.name(), encryptor.encrypt(table, keyOfSpace));
        }
        return rewritten;
    }
}

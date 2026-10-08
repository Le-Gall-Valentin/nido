package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.SentMailCode;
import com.nido.api.mfa.domain.port.out.MailCodeStorePort;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** MailCodeStorePort as the database behaves, for tests that follow a code through several steps. */
class InMemoryMailCodeStore implements MailCodeStorePort {

    private record Key(UUID userId, CodePurpose purpose) {}

    final Map<Key, SentMailCode> codes = new HashMap<>();

    @Override
    public Optional<SentMailCode> find(UUID userId, CodePurpose purpose) {
        return Optional.ofNullable(codes.get(new Key(userId, purpose)));
    }

    @Override
    public void replace(SentMailCode code) {
        codes.put(new Key(code.userId(), code.purpose()), new SentMailCode(code.userId(), code.purpose(),
            code.bindingHash(), code.codeHash(), 0, code.sentAt(), code.expiresAt()));
    }

    @Override
    public int recordFailure(UUID userId, CodePurpose purpose) {
        SentMailCode code = codes.get(new Key(userId, purpose));
        if (code == null) return 0;
        SentMailCode failed = new SentMailCode(userId, purpose, code.bindingHash(), code.codeHash(),
            code.failedAttempts() + 1, code.sentAt(), code.expiresAt());
        codes.put(new Key(userId, purpose), failed);
        return failed.failedAttempts();
    }

    @Override
    public void delete(UUID userId, CodePurpose purpose) {
        codes.remove(new Key(userId, purpose));
    }

    @Override
    public void deleteAll(UUID userId) {
        codes.keySet().removeIf(key -> key.userId().equals(userId));
    }

    @Override
    public int deleteExpired(Instant now) {
        int before = codes.size();
        codes.values().removeIf(code -> !code.expiresAt().isAfter(now));
        return before - codes.size();
    }
}

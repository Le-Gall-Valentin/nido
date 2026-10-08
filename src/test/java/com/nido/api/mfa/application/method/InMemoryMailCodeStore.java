package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.SentMailCode;
import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import com.nido.api.shared.model.TwoFactorPolicy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** MailCodeStorePort as the database behaves, for tests that follow a code through several steps. */
class InMemoryMailCodeStore implements MailCodeStorePort {

    private record Key(UUID userId, CodePurpose purpose) {}

    final Map<Key, SentMailCode> codes = new HashMap<>();
    /** Another request takes the code right after the next read: two requests carrying the same code at once. */
    boolean takenRightAfterNextFind;

    @Override
    public Optional<SentMailCode> find(UUID userId, CodePurpose purpose) {
        Optional<SentMailCode> found = Optional.ofNullable(codes.get(new Key(userId, purpose)));
        if (takenRightAfterNextFind) {
            takenRightAfterNextFind = false;
            codes.remove(new Key(userId, purpose));
        }
        return found;
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
    public boolean take(UUID userId, CodePurpose purpose, String codeHash) {
        SentMailCode live = codes.get(new Key(userId, purpose));
        return live != null && live.codeHash().equals(codeHash) && live.failedAttempts() < TwoFactorPolicy.MAX_ATTEMPTS
            && codes.remove(new Key(userId, purpose), live);
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

package com.synapsys.api.identity.domain.port.out;

import java.util.UUID;

public interface TotpDeletionPort {
    void deleteTotpData(UUID userId);
}
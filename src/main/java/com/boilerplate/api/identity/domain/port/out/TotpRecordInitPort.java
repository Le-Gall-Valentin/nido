package com.boilerplate.api.identity.domain.port.out;

import java.util.UUID;

public interface TotpRecordInitPort {
    void initForUser(UUID userId);
}
package com.nido.api.calendar.domain.port.out;

import java.util.UUID;

/** Whether an account may sign in: an address reads the space only as long as its owner may. */
public interface ActiveAccounts {
    boolean isActive(UUID userId);
}

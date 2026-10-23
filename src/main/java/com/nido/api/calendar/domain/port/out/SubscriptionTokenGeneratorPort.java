package com.nido.api.calendar.domain.port.out;

/** Makes the secret of a new subscription address: unguessable, and of the shape SubscriptionTokens recognises. */
public interface SubscriptionTokenGeneratorPort {
    String generate();
}

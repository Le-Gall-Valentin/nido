package com.nido.api.instance.domain.model;

import java.util.Optional;

/**
 * Whether the setup screen is due, and what the environment already decided for it — shown read-only.
 * Once the installation is set up it says nothing else: the route is public.
 */
public record SetupStatus(boolean required, Optional<String> lockedPublicUrl, boolean mailLocked) {
    public static SetupStatus done() {
        return new SetupStatus(false, Optional.empty(), false);
    }
}

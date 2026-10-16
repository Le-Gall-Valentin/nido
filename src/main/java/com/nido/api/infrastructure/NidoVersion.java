package com.nido.api.infrastructure;

import com.nido.api.NidoApiApplication;

import java.util.Optional;

/** The version of the running jar — set by the release, "dev" when run from the sources. */
public final class NidoVersion {

    private NidoVersion() {}

    public static String current() {
        return Optional.ofNullable(NidoApiApplication.class.getPackage().getImplementationVersion()).orElse("dev");
    }
}

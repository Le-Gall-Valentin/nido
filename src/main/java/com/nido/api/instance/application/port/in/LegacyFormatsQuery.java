package com.nido.api.instance.application.port.in;

/** Whether values of a format before 0.16.0 are still taken — see LegacyFormats. */
public interface LegacyFormatsQuery {

    boolean closed();
}

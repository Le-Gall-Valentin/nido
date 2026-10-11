package com.nido.api.instance.application.port.in;

/** Refuses values of a format before 0.16.0 for good, once a start brought every value to the current one. */
public interface CloseLegacyFormatsUseCase {

    /** Does nothing when they are closed already. */
    void closeForGood();
}

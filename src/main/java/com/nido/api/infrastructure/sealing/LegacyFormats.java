package com.nido.api.infrastructure.sealing;

/**
 * Whether values of a format before 0.16.0 are still taken: only until a start has brought every value to the current
 * format. From then on nothing legitimate can be of an earlier format, and one that shows up was written by someone with
 * access to the database — a ciphertext from an old backup, say. It is refused where it is read, and named at start;
 * never converted, so it cannot pass for the value of the row it was put in. Recorded in the database by the instance
 * module, which owns the installation's state.
 */
public interface LegacyFormats {

    /** Whether a start has brought every value to the current format: earlier formats are refused from then on. */
    boolean closed();

    /**
     * Records that every value is of the current format. Does nothing when that is recorded already. Not named
     * {@code close}: Spring would take that for the destroy method of the bean, and call it at every shutdown.
     */
    void closeForGood();
}

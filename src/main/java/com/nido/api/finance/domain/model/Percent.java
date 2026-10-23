package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A part of a whole in whole percent, rounded down, as a mail says it. */
final class Percent {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Percent() {}

    /** A whole of zero is reached by anything: 100. */
    static int of(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return 100;
        }
        return part.multiply(HUNDRED).divide(whole, 0, RoundingMode.DOWN).intValueExact();
    }
}

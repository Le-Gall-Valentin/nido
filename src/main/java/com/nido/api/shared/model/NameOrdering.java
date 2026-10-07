package com.nido.api.shared.model;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/** How a list of names reads in order, for a French reader: case and accents only break ties, "éclair" among the E. */
public final class NameOrdering {

    private NameOrdering() {}

    /** A new comparator per call: a Collator is cheap to make and synchronises its comparisons. */
    public static Comparator<String> comparator() {
        Collator french = Collator.getInstance(Locale.FRENCH);
        return french::compare;
    }
}

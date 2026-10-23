package com.nido.api.calendar.domain.model;

import java.util.Optional;

/**
 * What reading a downloaded calendar once says: the name it gives itself, and what it would weigh in memory (its text,
 * its properties and its time zones — see the reader). {@code weight} is what a followed calendar is refused for.
 */
public record FeedCheck(Optional<String> name, int weight) {

    /**
     * The heaviest calendar followed: about what 3 MB of a realistic file holds once read. Five of them fit the memory
     * given to all of them together.
     */
    public static final int MAX_WEIGHT = 24 * 1024 * 1024;

    public boolean tooHeavy() {
        return weight > MAX_WEIGHT;
    }
}

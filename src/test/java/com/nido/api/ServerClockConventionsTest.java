package com.nido.api;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * "Today" is not the server's to decide.
 *
 * <p>The application runs in UTC and the households that use it do not, so {@code LocalDate.now()}
 * in a handler answers with the date of the machine rather than the date of the space being read.
 * For the first two hours of every morning in Paris that is yesterday, and for the last hours of
 * every evening west of Greenwich it is tomorrow — and it decides what is due, what is late, and
 * which month the finance page opens on.
 *
 * <p>A handler must therefore ask {@code GetSpaceTodayUseCase}. This rule is what keeps the next one
 * from reaching for the clock, which is the obvious thing to write and silently wrong.
 */
class ServerClockConventionsTest {

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.nido.api");

    @Test
    void no_handler_asks_the_server_what_day_it_is() {
        noClasses()
            .that().resideInAPackage("com.nido.api..application..")
            .and().haveSimpleNameNotEndingWith("Test")
            .and().haveSimpleNameNotEndingWith("IT")
            // GetSpaceTodayHandler is the one place allowed to: it is what resolves a space's zone,
            // and it re-zones the injected clock before reading a date from it.
            .and().haveSimpleNameNotContaining("GetSpaceToday")
            .should().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDate.class, "now", java.time.Clock.class)
            .allowEmptyShould(true)
            .check(classes);
    }
}

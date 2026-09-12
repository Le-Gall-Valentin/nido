package com.nido.api.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    /**
     * One clock for the whole application, so that anything asking the time can be handed a fixed
     * one in a test instead of depending on when the suite happens to run. UTC as the base is
     * deliberate and invisible: every caller re-zones it to the space it is answering for, and a
     * base zone that is never used cannot quietly become the answer.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // No ObjectMapper bean here any more. There used to be a hand-built Jackson 2 one, used only by
    // the two security handlers below, while Spring MVC serialised everything else with the
    // Jackson 3 that Boot 4 configures — so the same ProblemDetail came out in two different
    // shapes depending on whether it was written by a filter or by a controller. The handlers now
    // take Boot's mapper, which is the one the rest of the API speaks.
}
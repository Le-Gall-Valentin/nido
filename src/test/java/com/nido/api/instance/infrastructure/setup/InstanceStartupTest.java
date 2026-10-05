package com.nido.api.instance.infrastructure.setup;

import com.nido.api.instance.domain.model.SetupCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceStartupTest {

    @Test
    void the_banner_carries_the_code_in_a_box_that_closes() {
        String banner = InstanceStartup.banner(new SetupCode("K7QM-3XRP-W9TD"));

        assertThat(banner).contains("Setup code: K7QM-3XRP-W9TD");
        assertThat(banner.lines().map(String::length).distinct()).hasSize(1);
        assertThat(banner.lines().findFirst().orElseThrow()).startsWith("╔").endsWith("╗");
    }
}

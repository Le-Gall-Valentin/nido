package com.nido.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@IntegrationTestConfig
class NidoApiApplicationTests {

    @Autowired ThreadPoolTaskScheduler taskScheduler;

    @Test
    void contextLoads() {
    }

    @Test
    void a_refresh_of_followed_calendars_never_holds_back_the_mail_sweep_and_the_purges() {
        // The refresh may take its thread for minutes, downloading: the others need one of their own.
        assertThat(taskScheduler.getPoolSize()).isEqualTo(2);
    }
}

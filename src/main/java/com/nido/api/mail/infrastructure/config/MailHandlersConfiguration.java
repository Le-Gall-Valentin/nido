package com.nido.api.mail.infrastructure.config;

import com.nido.api.infrastructure.config.ConditionalOnMailDisabled;
import com.nido.api.infrastructure.config.ConditionalOnMailEnabled;
import com.nido.api.mail.application.handler.DisabledSendMailHandler;
import com.nido.api.mail.application.handler.DispatchPendingMailsHandler;
import com.nido.api.mail.application.handler.MailAvailabilityHandler;
import com.nido.api.mail.application.handler.SendMailHandler;
import com.nido.api.mail.application.port.in.DispatchPendingMailsUseCase;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.RetryPolicy;
import com.nido.api.mail.domain.port.out.MailDispatchTriggerPort;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.mail.domain.port.out.MailRendererPort;
import com.nido.api.mail.domain.port.out.MailTransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The mail handlers, one set per mode. They are built here rather than scanned as
 * {@code @ApplicationService}s because the application layer may not carry Spring conditions, and
 * scanning would register both implementations of {@code SendMailUseCase} at once.
 *
 * <p>No handler asks "is mail on?": the configuration answers once, by choosing which one exists.
 */
@Configuration(proxyBeanMethods = false)
public class MailHandlersConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MailHandlersConfiguration.class);

    @Bean
    @ConditionalOnMailEnabled
    SendMailUseCase sendMailUseCase(MailRendererPort renderer, MailOutboxPort outbox,
                                    MailDispatchTriggerPort trigger, Clock clock) {
        return new SendMailHandler(renderer, outbox, trigger, clock);
    }

    @Bean
    @ConditionalOnMailDisabled
    SendMailUseCase disabledSendMailUseCase() {
        log.info("Mail is off (NIDO_SMTP_HOST is not set): no mail will be sent");
        return new DisabledSendMailHandler();
    }

    @Bean
    @ConditionalOnMailEnabled
    MailAvailabilityQuery mailAvailable() {
        return new MailAvailabilityHandler(true);
    }

    @Bean
    @ConditionalOnMailDisabled
    MailAvailabilityQuery mailUnavailable() {
        return new MailAvailabilityHandler(false);
    }

    @Bean
    @ConditionalOnMailEnabled
    DispatchPendingMailsUseCase dispatchPendingMailsUseCase(MailOutboxPort outbox, MailTransportPort transport, Clock clock) {
        return new DispatchPendingMailsHandler(outbox, transport, new RetryPolicy(), clock);
    }
}

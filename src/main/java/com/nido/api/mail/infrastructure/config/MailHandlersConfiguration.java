package com.nido.api.mail.infrastructure.config;

import com.nido.api.mail.application.handler.CancelPendingMailsHandler;
import com.nido.api.mail.application.handler.DispatchPendingMailsHandler;
import com.nido.api.mail.application.handler.MailAvailabilityHandler;
import com.nido.api.mail.application.handler.SendMailHandler;
import com.nido.api.mail.application.port.in.CancelPendingMailsUseCase;
import com.nido.api.mail.application.port.in.DispatchPendingMailsUseCase;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.RetryPolicy;
import com.nido.api.mail.domain.port.out.MailConfigurationPort;
import com.nido.api.mail.domain.port.out.MailDispatchTriggerPort;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.mail.domain.port.out.MailRendererPort;
import com.nido.api.mail.domain.port.out.MailTransportPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The mail handlers, built here rather than scanned so that the mail context's wiring reads in one
 * place. None of them is chosen by a switch any more: mail can be turned on and off from the settings
 * page, so each asks MailConfigurationPort at the moment it acts.
 */
@Configuration(proxyBeanMethods = false)
public class MailHandlersConfiguration {

    @Bean
    SendMailUseCase sendMailUseCase(MailConfigurationPort configuration, MailRendererPort renderer, MailOutboxPort outbox,
                                    MailDispatchTriggerPort trigger, Clock clock) {
        return new SendMailHandler(configuration, renderer, outbox, trigger, clock);
    }

    @Bean
    MailAvailabilityQuery mailAvailability(MailConfigurationPort configuration) {
        return new MailAvailabilityHandler(configuration);
    }

    @Bean
    DispatchPendingMailsUseCase dispatchPendingMailsUseCase(MailConfigurationPort configuration, MailOutboxPort outbox,
                                                            MailTransportPort transport, Clock clock) {
        return new DispatchPendingMailsHandler(configuration, outbox, transport, new RetryPolicy(), clock);
    }

    @Bean
    CancelPendingMailsUseCase cancelPendingMailsUseCase(MailOutboxPort outbox) {
        return new CancelPendingMailsHandler(outbox);
    }
}

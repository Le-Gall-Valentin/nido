package com.nido.api.mail.infrastructure.config;

import com.nido.api.infrastructure.config.ConditionalOnMailEnabled;
import com.nido.api.infrastructure.config.MailProperties;
import com.nido.api.mail.infrastructure.transport.SmtpSenderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** The checked settings and the one SMTP client — built only when mail is on. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMailEnabled
public class MailSettingsConfiguration {

    @Bean
    MailSettings mailSettings(MailProperties properties) {
        return MailSettings.from(properties);
    }

    @Bean
    JavaMailSenderImpl nidoMailSender(MailSettings settings) {
        return SmtpSenderFactory.create(settings);
    }
}

package com.nido.api.mail.infrastructure.render;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.domain.port.out.MailRendererPort;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** The renderer, with the public address of the moment: it can change from the settings page. */
@Component
public class MailRendererAdapter implements MailRendererPort {

    @Override
    public RenderedMail render(MailContent content, Locale locale, String appUrl) {
        return new ThymeleafMailRenderer(appUrl).render(content, locale);
    }
}

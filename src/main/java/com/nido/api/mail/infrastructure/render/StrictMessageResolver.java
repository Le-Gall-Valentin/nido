package com.nido.api.mail.infrastructure.render;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.StandardMessageResolver;

/**
 * Thymeleaf's message resolution — each template's messages from the {@code _fr}/{@code _en}
 * properties beside it, each fragment's from its own — except that a missing message is an error.
 * The standard resolver writes {@code ??key_fr??} into the mail instead, which is exactly the kind of
 * mistake that reaches an inbox unnoticed.
 */
public class StrictMessageResolver extends StandardMessageResolver {

    @Override
    public String createAbsentMessageRepresentation(ITemplateContext context, Class<?> origin, String key,
                                                    Object[] messageParameters) {
        throw new IllegalStateException("Mail message '" + key + "' is missing for " + context.getLocale()
            + " in template " + context.getTemplateData().getTemplate());
    }
}

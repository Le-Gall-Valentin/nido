package com.nido.api.mail.infrastructure.render;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * The logo every mail shows, attached inline and referenced by {@code cid:} from the layout, so a
 * client that blocks remote images still shows it. A PNG because Gmail does not render SVG.
 */
public final class MailBranding {

    public static final String LOGO_CID = "nido-mark";
    public static final Resource LOGO = new ClassPathResource("mail/assets/nido-mark.png");

    private MailBranding() {}
}

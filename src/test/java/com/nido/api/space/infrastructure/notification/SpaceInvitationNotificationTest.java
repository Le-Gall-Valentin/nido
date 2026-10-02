package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceInvitationNotificationTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");

    private static SpaceInvitationNotification invitation(String spaceName) {
        return new SpaceInvitationNotification("carol", "alice", spaceName, 7, new AppPath("/spaces"));
    }

    @Test
    void it_is_the_space_invitation_kind_written_as_a_space_mail() {
        assertThat(SpaceInvitationNotification.class.getAnnotation(NotificationKind.class).value())
            .isEqualTo("space.invitation");
        assertThat(invitation("Chez nous").template()).isEqualTo("space/invitation-received");
    }

    @Test
    void the_french_mail_says_who_invites_where_for_how_long_and_how_to_stop_it() {
        RenderedMail mail = RENDERER.render(invitation("Chez nous"), Locale.FRENCH);

        assertThat(mail.subject()).isEqualTo("alice vous invite dans l’espace Chez nous");
        assertThat(mail.text()).contains(
            "Bonjour carol,",
            "alice vous invite à rejoindre l’espace « Chez nous » sur Nido.",
            "Voir l’invitation\nhttps://nido.example/spaces",
            "L’invitation est valable 7 jours.",
            "Vous recevez ce mail parce que cette notification est activée dans vos préférences.",
            "Gérer mes notifications\nhttps://nido.example/account/preferences");
    }

    @Test
    void the_english_mail_says_the_same() {
        RenderedMail mail = RENDERER.render(invitation("Our place"), Locale.ENGLISH);

        assertThat(mail.subject()).isEqualTo("alice invited you to the space Our place");
        assertThat(mail.text()).contains(
            "Hello carol,",
            "alice invited you to join the space “Our place” on Nido.",
            "View the invitation\nhttps://nido.example/spaces",
            "The invitation is valid for 7 days.",
            "Manage my notifications\nhttps://nido.example/account/preferences");
    }

    @Test
    void a_space_name_is_text_not_markup() {
        RenderedMail mail = RENDERER.render(invitation("<b>Chez nous</b>"), Locale.FRENCH);

        assertThat(mail.html()).doesNotContain("<b>Chez nous</b>").contains("&lt;b&gt;Chez nous&lt;/b&gt;");
    }
}

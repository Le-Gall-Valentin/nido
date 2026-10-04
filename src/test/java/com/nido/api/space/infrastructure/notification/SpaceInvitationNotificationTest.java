package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.space.domain.model.InvitationCancellation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
            "Vous ne voulez plus recevoir ce type de mail ?",
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

    private static InvitationCancelledNotification cancelled(InvitationCancellation reason, String spaceName) {
        return new InvitationCancelledNotification("carol", "alice", spaceName, reason, new AppPath("/spaces"));
    }

    @Test
    void a_cancelled_invitation_is_its_own_kind() {
        assertThat(InvitationCancelledNotification.class.getAnnotation(NotificationKind.class).value())
            .isEqualTo("space.invitation-cancelled");
        assertThat(cancelled(InvitationCancellation.REVOKED, "Chez nous").template()).isEqualTo("space/invitation-cancelled");
    }

    @Test
    void the_cancellation_says_why_in_french() {
        RenderedMail revoked = RENDERER.render(cancelled(InvitationCancellation.REVOKED, "Chez nous"), Locale.FRENCH);
        RenderedMail deleted = RENDERER.render(cancelled(InvitationCancellation.SPACE_DELETED, "Chez nous"), Locale.FRENCH);
        RenderedMail ownerLeft = RENDERER.render(cancelled(InvitationCancellation.OWNER_LEFT_NIDO, "Chez nous"), Locale.FRENCH);

        assertThat(revoked.subject()).isEqualTo("Votre invitation dans Chez nous est annulée");
        assertThat(revoked.text()).contains("Bonjour carol,",
            "alice a annulé votre invitation à rejoindre l’espace « Chez nous ».",
            "Voir mes espaces\nhttps://nido.example/spaces");
        assertThat(deleted.text()).contains("alice a supprimé l’espace « Chez nous » : votre invitation n’est plus valable.");
        assertThat(ownerLeft.text())
            .contains("alice a quitté Nido et l’espace « Chez nous » a été supprimé : votre invitation n’est plus valable.");
    }

    @Test
    void the_cancellation_says_why_in_english() {
        RenderedMail revoked = RENDERER.render(cancelled(InvitationCancellation.REVOKED, "Our place"), Locale.ENGLISH);
        RenderedMail deleted = RENDERER.render(cancelled(InvitationCancellation.SPACE_DELETED, "Our place"), Locale.ENGLISH);
        RenderedMail ownerLeft = RENDERER.render(cancelled(InvitationCancellation.OWNER_LEFT_NIDO, "Our place"), Locale.ENGLISH);

        assertThat(revoked.subject()).isEqualTo("Your invitation to Our place was cancelled");
        assertThat(revoked.text()).contains("alice cancelled your invitation to join the space “Our place”.", "See my spaces");
        assertThat(deleted.text()).contains("alice deleted the space “Our place”: your invitation is no longer valid.");
        assertThat(ownerLeft.text())
            .contains("alice left Nido and the space “Our place” was deleted: your invitation is no longer valid.");
    }

    /** A reason added without its sentence would fail the gesture itself: StrictMessageResolver throws. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(InvitationCancellation.class)
    void every_reason_has_its_sentence(InvitationCancellation reason) {
        assertThat(RENDERER.render(cancelled(reason, "Chez nous"), Locale.FRENCH).text()).contains("Chez nous");
        assertThat(RENDERER.render(cancelled(reason, "Chez nous"), Locale.ENGLISH).text()).contains("Chez nous");
    }
}

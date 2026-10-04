package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceChangeMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath MEMBERS = new AppPath("/s/42/members");
    private static final AppPath SPACES = new AppPath("/spaces");

    private static String kindOf(Class<?> record) {
        return record.getAnnotation(NotificationKind.class).value();
    }

    @Test
    void each_mail_declares_its_kind_and_template() {
        assertThat(kindOf(SpaceDeletedNotification.class)).isEqualTo("space.deleted");
        assertThat(kindOf(RoleChangedNotification.class)).isEqualTo("space.role-changed");
        assertThat(kindOf(OwnershipReceivedNotification.class)).isEqualTo("space.ownership-received");
        assertThat(kindOf(OwnerChangedNotification.class)).isEqualTo("space.owner-changed");
        assertThat(new SpaceDeletedNotification("u", "a", "s", SPACES).template()).isEqualTo("space/deleted");
        assertThat(new RoleChangedNotification("u", "a", "s", SpaceRole.VIEWER, SpaceRole.MEMBER, MEMBERS).template())
            .isEqualTo("space/role-changed");
        assertThat(new OwnershipReceivedNotification("u", "a", "s", false, MEMBERS).template()).isEqualTo("space/ownership-received");
        assertThat(new OwnerChangedNotification("u", "a", "n", "s", false, MEMBERS).template()).isEqualTo("space/owner-changed");
    }

    @Test
    void the_space_was_deleted() {
        RenderedMail fr = RENDERER.render(new SpaceDeletedNotification("carol", "alice", "Chez nous", SPACES), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SpaceDeletedNotification("carol", "alice", "Our place", SPACES), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("L’espace Chez nous a été supprimé");
        assertThat(fr.text()).contains("alice a supprimé l’espace « Chez nous » et tout ce qu’il contenait.",
            "Voir mes espaces\nhttps://nido.example/spaces");
        assertThat(en.subject()).isEqualTo("The space Our place was deleted");
        assertThat(en.text()).contains("alice deleted the space “Our place” and everything in it.");
    }

    @Test
    void a_role_change_says_both_roles_in_words() {
        RenderedMail fr = RENDERER.render(
            new RoleChangedNotification("bob", "alice", "Chez nous", SpaceRole.VIEWER, SpaceRole.MEMBER, MEMBERS), Locale.FRENCH);
        RenderedMail en = RENDERER.render(
            new RoleChangedNotification("bob", "alice", "Our place", SpaceRole.MEMBER, SpaceRole.ADMIN, MEMBERS), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("Votre rôle dans Chez nous a changé");
        assertThat(fr.text()).contains("alice a changé votre rôle dans l’espace « Chez nous » : Lecture seule → Membre.");
        assertThat(en.subject()).isEqualTo("Your role in Our place changed");
        assertThat(en.text()).contains("alice changed your role in the space “Our place”: Member → Admin.");
    }

    /** A role added to SpaceRole without its words would fail the role change itself: StrictMessageResolver throws. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(SpaceRole.class)
    void every_role_has_its_word_in_both_languages(SpaceRole role) {
        for (Locale locale : List.of(Locale.FRENCH, Locale.ENGLISH)) {
            RenderedMail mail = RENDERER.render(
                new RoleChangedNotification("bob", "alice", "Chez nous", role, role, MEMBERS), locale);

            assertThat(mail.text()).as("%s in %s", role, locale).contains(" → ").doesNotContain("role.");
        }
    }

    @Test
    void ownership_received_from_someone_or_from_someone_who_left_nido() {
        RenderedMail transfer = RENDERER.render(new OwnershipReceivedNotification("bob", "alice", "Chez nous", false, MEMBERS), Locale.FRENCH);
        RenderedMail succession = RENDERER.render(new OwnershipReceivedNotification("bob", "alice", "Chez nous", true, MEMBERS), Locale.FRENCH);
        RenderedMail english = RENDERER.render(new OwnershipReceivedNotification("bob", "alice", "Our place", true, MEMBERS), Locale.ENGLISH);

        assertThat(transfer.subject()).isEqualTo("Vous êtes propriétaire de Chez nous");
        assertThat(transfer.text()).contains("alice vous a transmis la propriété de l’espace « Chez nous ».")
            .doesNotContain("a quitté Nido");
        assertThat(succession.text())
            .contains("alice a quitté Nido : vous êtes maintenant propriétaire de l’espace « Chez nous ».")
            .doesNotContain("vous a transmis");
        assertThat(english.subject()).isEqualTo("You own Our place now");
        assertThat(english.text()).contains("alice left Nido: you own the space “Our place” now.");
    }

    @Test
    void a_new_owner_announced_to_the_others() {
        RenderedMail transfer = RENDERER.render(new OwnerChangedNotification("carol", "alice", "bob", "Chez nous", false, MEMBERS), Locale.FRENCH);
        RenderedMail succession = RENDERER.render(new OwnerChangedNotification("carol", "alice", "bob", "Chez nous", true, MEMBERS), Locale.FRENCH);
        RenderedMail english = RENDERER.render(new OwnerChangedNotification("carol", "alice", "bob", "Our place", true, MEMBERS), Locale.ENGLISH);

        assertThat(transfer.subject()).isEqualTo("bob est propriétaire de Chez nous");
        assertThat(transfer.text()).contains("alice a transmis la propriété de l’espace « Chez nous » à bob.");
        assertThat(succession.text())
            .contains("alice a quitté Nido : bob est maintenant propriétaire de l’espace « Chez nous ».");
        assertThat(english.subject()).isEqualTo("bob owns Our place now");
        assertThat(english.text()).contains("alice left Nido: bob owns the space “Our place” now.");
    }
}

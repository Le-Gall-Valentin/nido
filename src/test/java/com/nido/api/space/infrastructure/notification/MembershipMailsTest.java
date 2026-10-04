package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath MEMBERS = new AppPath("/s/42/members");
    private static final AppPath SPACES = new AppPath("/spaces");

    private static String kindOf(Class<?> record) {
        return record.getAnnotation(NotificationKind.class).value();
    }

    @Test
    void each_mail_declares_its_kind_and_template() {
        assertThat(kindOf(MemberJoinedNotification.class)).isEqualTo("space.member-joined");
        assertThat(kindOf(MemberLeftNotification.class)).isEqualTo("space.member-left");
        assertThat(kindOf(MemberRemovedNotification.class)).isEqualTo("space.member-removed");
        assertThat(kindOf(RemovedFromSpaceNotification.class)).isEqualTo("space.removed");
        assertThat(new MemberJoinedNotification("u", "m", "s", MEMBERS).template()).isEqualTo("space/member-joined");
        assertThat(new MemberLeftNotification("u", "m", "s", false, MEMBERS).template()).isEqualTo("space/member-left");
        assertThat(new MemberRemovedNotification("u", "a", "m", "s", MEMBERS).template()).isEqualTo("space/member-removed");
        assertThat(new RemovedFromSpaceNotification("u", "a", "s", SPACES).template()).isEqualTo("space/removed");
    }

    @Test
    void someone_joined() {
        RenderedMail fr = RENDERER.render(new MemberJoinedNotification("carol", "bob", "Chez nous", MEMBERS), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new MemberJoinedNotification("carol", "bob", "Our place", MEMBERS), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob a rejoint Chez nous");
        assertThat(fr.text()).contains("Bonjour carol,", "bob a rejoint l’espace « Chez nous ».",
            "Voir les membres\nhttps://nido.example/s/42/members");
        assertThat(en.subject()).isEqualTo("bob joined Our place");
        assertThat(en.text()).contains("Hello carol,", "bob joined the space “Our place”.", "See the members");
    }

    @Test
    void someone_left() {
        RenderedMail fr = RENDERER.render(new MemberLeftNotification("carol", "bob", "Chez nous", false, MEMBERS), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new MemberLeftNotification("carol", "bob", "Our place", false, MEMBERS), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob a quitté Chez nous");
        assertThat(fr.text()).contains("bob a quitté l’espace « Chez nous ».").doesNotContain("Nido et");
        assertThat(en.subject()).isEqualTo("bob left Our place");
        assertThat(en.text()).contains("bob left the space “Our place”.");
    }

    @Test
    void someone_left_nido() {
        RenderedMail fr = RENDERER.render(new MemberLeftNotification("carol", "bob", "Chez nous", true, MEMBERS), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new MemberLeftNotification("carol", "bob", "Our place", true, MEMBERS), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob a quitté Chez nous");
        assertThat(fr.text()).contains("bob a quitté Nido et ne fait plus partie de l’espace « Chez nous ».");
        assertThat(en.text()).contains("bob left Nido and is no longer in the space “Our place”.");
    }

    @Test
    void someone_was_removed_by_someone() {
        RenderedMail fr = RENDERER.render(new MemberRemovedNotification("carol", "alice", "bob", "Chez nous", MEMBERS), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new MemberRemovedNotification("carol", "alice", "bob", "Our place", MEMBERS), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob ne fait plus partie de Chez nous");
        assertThat(fr.text()).contains("alice a retiré bob de l’espace « Chez nous ».");
        assertThat(en.subject()).isEqualTo("bob is no longer in Our place");
        assertThat(en.text()).contains("alice removed bob from the space “Our place”.");
    }

    @Test
    void you_were_removed_and_the_link_no_longer_points_into_the_space() {
        RenderedMail fr = RENDERER.render(new RemovedFromSpaceNotification("bob", "alice", "Chez nous", SPACES), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new RemovedFromSpaceNotification("bob", "alice", "Our place", SPACES), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("Vous ne faites plus partie de Chez nous");
        assertThat(fr.text()).contains("Bonjour bob,",
            "alice vous a retiré de l’espace « Chez nous ». Vous n’y avez plus accès.",
            "Voir mes espaces\nhttps://nido.example/spaces");
        assertThat(en.subject()).isEqualTo("You are no longer in Our place");
        assertThat(en.text()).contains("alice removed you from the space “Our place”. You no longer have access to it.",
            "See my spaces");
    }
}

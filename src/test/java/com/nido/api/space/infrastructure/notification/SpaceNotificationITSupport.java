package com.nido.api.space.infrastructure.notification;

import com.nido.api.SharedGreenMail;
import com.nido.api.TestSpaces;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the space notification tests set up and read back: accounts addressed {@code <username>@test.local},
 * a shared space and its memberships, and the mails the shared SMTP server received.
 */
final class SpaceNotificationITSupport {

    private final UserIdentityJpaRepository users;
    private final SpaceJpaRepository spaces;
    private final SpaceMemberJpaRepository members;

    SpaceNotificationITSupport(UserIdentityJpaRepository users, SpaceJpaRepository spaces,
                               SpaceMemberJpaRepository members) {
        this.users = users;
        this.spaces = spaces;
        this.members = members;
    }

    UUID saveUser(String username, String language) {
        return saveUser(username, language, Role.USER);
    }

    UUID saveUser(String username, String language, Role role) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setRole(role);
        user.setLanguage(language);
        return users.saveAndFlush(user).getId();
    }

    UUID saveSharedSpace(String name, UUID creatorId) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        space.setCreatedBy(creatorId);
        return spaces.saveAndFlush(space).getId();
    }

    void saveMembership(UUID spaceId, UUID userId, SpaceRole role) {
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(spaceId);
        member.setUserId(userId);
        member.setRole(role);
        members.saveAndFlush(member);
    }

    /** Waits for {@code count} mails, then returns every mail received by recipient address — two to one address fail. */
    static Map<String, MimeMessage> mailsByRecipient(int count) {
        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, count)).isTrue();
        return Arrays.stream(SharedGreenMail.server().getReceivedMessages())
            .collect(Collectors.toMap(SpaceNotificationITSupport::recipientOf, message -> message));
    }

    static Map<String, String> subjectsByRecipient(int count) {
        return mailsByRecipient(count).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> subjectOf(entry.getValue())));
    }

    static String recipientOf(MimeMessage message) {
        try {
            return ((InternetAddress) message.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String subjectOf(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The plain-text part, wherever it sits in the multipart tree. */
    static String textOf(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String text = textOf(multipart.getBodyPart(i));
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }
}

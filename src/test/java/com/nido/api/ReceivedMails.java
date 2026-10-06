package com.nido.api;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Arrays;
import java.util.List;

/** The mails {@link SharedGreenMail} received for one address, waited for: dispatch runs on its own thread. */
public final class ReceivedMails {

    private static final int ATTEMPTS = 100;
    private static final long PAUSE_MILLIS = 100;

    private ReceivedMails() {}

    /** The first mail received for this address, waited for up to ten seconds. */
    public static MimeMessage to(String address) throws InterruptedException {
        return allTo(address, 1).getFirst();
    }

    /** The mails received for this address, once there are at least {@code count}, waited for up to ten seconds. */
    public static List<MimeMessage> allTo(String address, int count) throws InterruptedException {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            List<MimeMessage> found = Arrays.stream(SharedGreenMail.server().getReceivedMessages())
                .filter(message -> recipientOf(message).equals(address))
                .toList();
            if (found.size() >= count) {
                return found;
            }
            Thread.sleep(PAUSE_MILLIS);
        }
        throw new AssertionError("Fewer than " + count + " mail(s) to " + address + " within ten seconds");
    }

    public static String recipientOf(MimeMessage message) {
        try {
            return ((InternetAddress) message.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String subjectOf(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The plain-text part, wherever it sits in the multipart tree. */
    public static String textOf(Part part) throws Exception {
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

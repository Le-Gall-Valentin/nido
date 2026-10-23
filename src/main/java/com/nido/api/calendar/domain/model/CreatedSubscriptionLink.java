package com.nido.api.calendar.domain.model;

/**
 * A subscription address just given, shown this once: its secret is kept nowhere else, only its digest. {@code url} is
 * the address a calendar application reads, {@code webcalUrl} the same that opens one by a click.
 */
public record CreatedSubscriptionLink(SubscriptionLink link, String url, String webcalUrl) {

    /** The address of {@code token} on the installation answering at {@code publicUrl} (no trailing '/'). */
    public static CreatedSubscriptionLink at(String publicUrl, SubscriptionLink link, String token) {
        String url = publicUrl + "/ical/" + token + ".ics";
        return new CreatedSubscriptionLink(link, url, "webcal" + url.substring(url.indexOf("://")));
    }

    /** Never the address: it is the secret. */
    @Override
    public String toString() {
        return "CreatedSubscriptionLink[link=" + link + "]";
    }
}

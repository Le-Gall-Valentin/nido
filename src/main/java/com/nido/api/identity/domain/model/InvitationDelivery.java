package com.nido.api.identity.domain.model;

/** How an account's invitation reached it: by mail, or as a link the administrator passes on, shown once. */
public sealed interface InvitationDelivery {

    record Mailed() implements InvitationDelivery {}

    /** @param url the full link, or its path when the installation does not know its public address */
    record Link(String url) implements InvitationDelivery {

        /** Without the token: a record holding this link may end up in a log line. */
        @Override
        public String toString() {
            int fragment = url.indexOf('#');
            return "Link[url=" + (fragment < 0 ? url : url.substring(0, fragment) + "#…") + "]";
        }
    }
}

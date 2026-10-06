package com.nido.api.authentication.application.dto;

/** How an invitation reached its account: by mail, or as a link for the administrator to pass on. */
public sealed interface InvitationDelivery {

    record Mailed() implements InvitationDelivery {}

    /**
     * @param url the full link when the public address is known, otherwise its path — shown once, never stored
     */
    record Link(String url) implements InvitationDelivery {

        /** Without the token: a record holding this link may end up in a log line. */
        @Override
        public String toString() {
            int fragment = url.indexOf('#');
            return "Link[url=" + (fragment < 0 ? url : url.substring(0, fragment) + "#…") + "]";
        }
    }
}

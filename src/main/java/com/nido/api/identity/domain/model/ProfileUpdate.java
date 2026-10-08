package com.nido.api.identity.domain.model;

/** What came of a profile change: saved, or waiting for the code sent to the new address. */
public sealed interface ProfileUpdate permits ProfileUpdate.Saved, ProfileUpdate.EmailCodeSent {

    record Saved() implements ProfileUpdate {}

    /** Nothing saved yet: the same change comes back with the code. */
    record EmailCodeSent(String sentTo, long resendAfterSeconds) implements ProfileUpdate {}
}

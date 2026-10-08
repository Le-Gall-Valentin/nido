package com.nido.api.identity.domain.model;

/** What came of a profile change: saved, or waiting for the code sent to the new address. */
public sealed interface ProfileUpdate permits ProfileUpdate.Saved, ProfileUpdate.SavedMailMethodRemoved, ProfileUpdate.EmailCodeSent {

    record Saved() implements ProfileUpdate {}

    /** Saved while mail is off, and the code by mail turned off with it: nothing could prove the new address. */
    record SavedMailMethodRemoved() implements ProfileUpdate {}

    /** Nothing saved yet: the same change comes back with the code. */
    record EmailCodeSent(String sentTo, long resendAfterSeconds) implements ProfileUpdate {}
}

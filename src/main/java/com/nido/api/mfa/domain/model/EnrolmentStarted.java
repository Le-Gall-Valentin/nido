package com.nido.api.mfa.domain.model;

/** What turning a method on starts with — not the same thing for each method. */
public sealed interface EnrolmentStarted permits EnrolmentStarted.AppEnrolment, EnrolmentStarted.MailEnrolment {

    /** A secret to scan as a QR code, or to type. */
    record AppEnrolment(String secret, String otpauthUri) implements EnrolmentStarted {}

    /** A code sent to the account's address, and when another can be asked for. */
    record MailEnrolment(String sentTo, long resendAfterSeconds) implements EnrolmentStarted {}
}

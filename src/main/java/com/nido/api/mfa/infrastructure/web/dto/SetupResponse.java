package com.nido.api.mfa.infrastructure.web.dto;

/** What starting a method answers: the app's secret to scan, or where the mail's code went. */
public sealed interface SetupResponse permits AppSetupResponse, MailSetupResponse {}

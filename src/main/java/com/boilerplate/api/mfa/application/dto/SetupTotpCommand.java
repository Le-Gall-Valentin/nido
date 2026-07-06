package com.boilerplate.api.mfa.application.dto;

import java.util.UUID;

public record SetupTotpCommand(UUID userId, String email) {}

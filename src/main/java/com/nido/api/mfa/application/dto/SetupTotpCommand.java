package com.nido.api.mfa.application.dto;

import java.util.UUID;

public record SetupTotpCommand(UUID userId, String email) {}

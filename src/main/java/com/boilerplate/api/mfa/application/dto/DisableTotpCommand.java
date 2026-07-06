package com.boilerplate.api.mfa.application.dto;

import java.util.UUID;

public record DisableTotpCommand(UUID userId, String code) {}

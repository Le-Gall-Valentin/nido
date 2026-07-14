package com.nido.api.mfa.application.dto;

import java.util.UUID;

public record ConfirmTotpCommand(UUID userId, String code) {}

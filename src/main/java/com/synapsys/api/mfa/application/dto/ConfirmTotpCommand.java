package com.synapsys.api.mfa.application.dto;

import java.util.UUID;

public record ConfirmTotpCommand(UUID userId, String code) {}

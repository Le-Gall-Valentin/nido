package com.synapsys.api.mfa.domain.model;

import java.util.Optional;
import java.util.UUID;

public record UserTotpProfile(UUID id, boolean totpEnabled, Optional<String> totpSecret) {}
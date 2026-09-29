package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.Language;

import java.util.UUID;

public interface ChangeMyLanguageUseCase {
    void changeLanguage(UUID userId, Language language);
}

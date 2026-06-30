package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.identity.domain.port.out.TotpDeletionPort;
import com.synapsys.api.mfa.application.port.in.DeleteTotpDataUseCase;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TotpDeletionAdapter implements TotpDeletionPort {

    private final DeleteTotpDataUseCase deleteTotpDataUseCase;

    public TotpDeletionAdapter(DeleteTotpDataUseCase deleteTotpDataUseCase) {
        this.deleteTotpDataUseCase = deleteTotpDataUseCase;
    }

    @Override
    public void deleteTotpData(UUID userId) {
        deleteTotpDataUseCase.deleteUserData(userId);
    }
}
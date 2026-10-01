package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.ReceivedInvitationView;

import java.util.List;
import java.util.UUID;

public interface ListMyInvitationsUseCase {
    List<ReceivedInvitationView> listMine(UUID userId);
}

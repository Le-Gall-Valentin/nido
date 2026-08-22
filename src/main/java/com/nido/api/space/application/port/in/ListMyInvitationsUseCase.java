package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.ReceivedInvitationView;

import java.util.List;

public interface ListMyInvitationsUseCase {
    List<ReceivedInvitationView> listMine(String email);
}

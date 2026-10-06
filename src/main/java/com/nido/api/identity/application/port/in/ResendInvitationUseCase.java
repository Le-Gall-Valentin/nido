package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.ResendInvitationCommand;

public interface ResendInvitationUseCase {
    /** A new link for an account that has not chosen its password yet; the previous one stops working. */
    InvitationDelivery resend(ResendInvitationCommand command);
}

package com.nido.api.authentication.application.port.in;

public interface CheckAccountInvitationUseCase {

    /**
     * @return the username of the account the link invites
     * @throws com.nido.api.authentication.domain.model.AuthenticationException.InvalidInvitationToken when the link
     *         expired, was used, was replaced, never existed, or its account is deactivated or deleted
     */
    String check(String token);
}

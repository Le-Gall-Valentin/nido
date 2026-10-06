package com.nido.api.authentication.application.port.in;

public interface AcceptAccountInvitationUseCase {

    /**
     * Sets the account's first password and ends its invitation. Signs nobody in.
     *
     * @throws com.nido.api.authentication.domain.model.AuthenticationException.InvalidInvitationToken see
     *         {@link CheckAccountInvitationUseCase#check}
     */
    void accept(String token, String password);
}

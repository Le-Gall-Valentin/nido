package com.nido.api.authentication.application.service;

import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.shared.annotation.ApplicationService;

/**
 * What follows a new password, whoever set it — the holder from the account page, or a reset link.
 * A password is changed in reaction to a suspected compromise, so every way in the old one opened is
 * closed: a reset link sent before, a refresh token that would mint access tokens for thirty more
 * days, an access token still inside its fifteen minutes. Then the holder is told, in case it was not
 * them.
 *
 * <p>One place for both callers on purpose: each step closes a door the others leave open, and a step
 * added here — or forgotten — is added or forgotten for both.
 *
 * <p>Runs inside the caller's transaction, except the refresh-token revocation, which commits on its
 * own (REQUIRES_NEW, see RefreshTokenRepositoryAdapter). Should the caller fail at commit afterwards,
 * the tokens stay revoked while the password stays unchanged: the holder signs in again with the old
 * password — the harmless direction of the two.
 */
@ApplicationService
public class PasswordChangeConsequences {

    private final PasswordResetTokenRepository resetTokens;
    private final RefreshTokenRevocationPort refreshTokens;
    private final IssuedTokenCutoffPort cutoff;
    private final AccountMailPort mail;

    public PasswordChangeConsequences(PasswordResetTokenRepository resetTokens, RefreshTokenRevocationPort refreshTokens,
                                      IssuedTokenCutoffPort cutoff, AccountMailPort mail) {
        this.resetTokens = resetTokens;
        this.refreshTokens = refreshTokens;
        this.cutoff = cutoff;
        this.mail = mail;
    }

    public void apply(AccountContact account) {
        resetTokens.deleteAllForUser(account.userId());
        refreshTokens.revokeAllForUser(account.userId());
        cutoff.cutOffNow(account.userId());
        mail.passwordChanged(account);
    }
}

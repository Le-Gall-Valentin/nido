package com.synapsys.api.authentication.domain.port.out;

import java.util.UUID;

import com.synapsys.api.authentication.domain.model.TotpVerificationResult;

/**
 * Port inter-BC : authentication définit, mfa implémente via authentication.infrastructure.
 * Valide le code TOTP et le consomme atomiquement (anti-replay).
 * REPLAYED = code valide mais déjà utilisé (ne pas incrémenter les tentatives).
 * INVALID = code incorrect ou profil introuvable.
 */
public interface MfaTotpVerifierPort {
    TotpVerificationResult verifyAndConsume(UUID userId, String code);
}
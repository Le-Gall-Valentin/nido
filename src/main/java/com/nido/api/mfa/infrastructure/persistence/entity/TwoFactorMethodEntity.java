package com.nido.api.mfa.infrastructure.persistence.entity;

import com.nido.api.shared.model.TwoFactorMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/** One method an account has turned on. A row exists only while the method is on. */
@Entity
@Table(name = "two_factor_methods")
@IdClass(TwoFactorMethodEntity.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TwoFactorMethodEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 16)
    private TwoFactorMethod method;

    /** Encrypted with the account's key — only the application method has one. */
    @Column(name = "secret")
    private String secret;

    public TwoFactorMethodEntity(UUID userId, TwoFactorMethod method, String secret) {
        this.userId = userId;
        this.method = method;
        this.secret = secret;
    }

    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private UUID userId;
        private TwoFactorMethod method;
    }
}

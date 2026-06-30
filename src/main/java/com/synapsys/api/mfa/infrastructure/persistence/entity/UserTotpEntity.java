package com.synapsys.api.mfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "user_totp")
@Getter @Setter @NoArgsConstructor
public class UserTotpEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "totp_secret", length = 256)
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;
}
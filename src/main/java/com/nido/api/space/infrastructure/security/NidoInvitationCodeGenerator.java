package com.nido.api.space.infrastructure.security;

import com.nido.api.space.domain.port.out.InvitationCodeGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class NidoInvitationCodeGenerator implements InvitationCodeGeneratorPort {

    // Ni I ni O : le code est transmis de vive voix ou recopié à la main.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
    private static final String PREFIX = "NIDO-";
    private static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}

package com.synapsys.api.identity.application.port.in;

public interface SeedUseCase {
    void seedInitialSuperAdmin(String username, String email, String password);
}
package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.shared.model.Role;
import com.synapsys.api.shared.security.AuthenticatedUser;
import com.synapsys.api.shared.security.RateLimitPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails, RateLimitPrincipal {

    private final UUID userId;
    private final Role role;
    private final String email;
    private final Collection<GrantedAuthority> authorities;

    public CustomUserDetails(UUID userId, Role role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(userId, role, email);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public String getPassword() {
        return "";
    }

    // Le compte est stateless JWT — is_active est vérifié à la connexion dans LoginHandler
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
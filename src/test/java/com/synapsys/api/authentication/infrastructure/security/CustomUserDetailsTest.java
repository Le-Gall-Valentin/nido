package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    private final UUID userId = UUID.randomUUID();
    private final CustomUserDetails details = new CustomUserDetails(userId, Role.USER, "user@test.com");

    @Test
    void getPassword_returnsEmptyString() {
        assertThat(details.getPassword()).isEqualTo("");
    }

    @Test
    void getUsername_returnsUserIdAsString() {
        assertThat(details.getUsername()).isEqualTo(userId.toString());
    }

    @Test
    void getAuthorities_containsRoleAuthority() {
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void adminRole_hasAdminAuthority() {
        CustomUserDetails admin = new CustomUserDetails(UUID.randomUUID(), Role.ADMIN, "admin@test.com");
        assertThat(admin.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void accountStatusFlags_areAllTrue() {
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void getUserId_returnsConstructorValue() {
        assertThat(details.getUserId()).isEqualTo(userId);
    }

    @Test
    void getRole_returnsConstructorValue() {
        assertThat(details.getRole()).isEqualTo(Role.USER);
    }
}
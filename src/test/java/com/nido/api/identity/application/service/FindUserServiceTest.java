package com.nido.api.identity.application.service;

import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUserServiceTest {

    @Mock UserRepository userRepository;

    private FindUserService service;

    private final User user = new User(
        UUID.randomUUID(), "alice", "alice@test.com", Role.USER, true, Instant.now()
    , null);

    @BeforeEach
    void setUp() {
        service = new FindUserService(userRepository);
    }

    @Test
    void a_typed_username_finds_the_account_by_username_exactly() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(service.findByIdentifier("alice")).contains(user);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void a_typed_address_finds_the_account_by_address() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.findByIdentifier("alice@test.com")).contains(user);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void surrounding_spaces_and_capitals_do_not_matter_for_an_address() {
        // Mobile keyboards add a space after autocompleting an address.
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.findByIdentifier(" Alice@Test.COM ")).contains(user);
    }

    @Test
    void a_blank_identifier_finds_nobody_without_asking() {
        assertThat(service.findByIdentifier("   ")).isEmpty();
        assertThat(service.findByIdentifier(null)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void findById_existingUser_returnsUser() {
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThat(service.findById(user.id())).contains(user);
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThat(service.findById(unknown)).isEmpty();
    }
}
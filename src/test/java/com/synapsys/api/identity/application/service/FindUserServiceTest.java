package com.synapsys.api.identity.application.service;

import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUserServiceTest {

    @Mock UserRepository userRepository;

    private FindUserService service;

    private final User user = new User(
        UUID.randomUUID(), "alice", "alice@test.com", Role.USER, true, Instant.now()
    );

    @BeforeEach
    void setUp() {
        service = new FindUserService(userRepository);
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(service.findByUsername("alice")).contains(user);
    }

    @Test
    void findByUsername_unknownUser_returnsEmpty() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThat(service.findByUsername("unknown")).isEmpty();
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
package com.synapsys.api.identity.application.service;

import com.synapsys.api.identity.application.port.in.FindUserUseCase;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class FindUserService implements FindUserUseCase {

    private final UserRepository userRepository;

    public FindUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> findByIds(Collection<UUID> ids) {
        return userRepository.findByIds(ids);
    }
}

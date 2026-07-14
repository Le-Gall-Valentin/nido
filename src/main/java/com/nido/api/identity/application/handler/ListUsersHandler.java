package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.ListUsersUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.model.UserAdminView;
import com.nido.api.identity.domain.port.out.TotpStatusPort;
import com.nido.api.identity.domain.port.out.UserAdminPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.PageResult;
import com.nido.api.shared.model.SortRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
public class ListUsersHandler implements ListUsersUseCase {

    private final UserAdminPort userAdminPort;
    private final TotpStatusPort totpStatusPort;

    public ListUsersHandler(UserAdminPort userAdminPort, TotpStatusPort totpStatusPort) {
        this.userAdminPort = userAdminPort;
        this.totpStatusPort = totpStatusPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserAdminView> listUsers(int page, int size, SortRequest sort, String search) {
        PageResult<User> result = userAdminPort.findAll(page, size, sort, search);
        if (result.content().isEmpty()) {
            return new PageResult<>(List.of(), result.totalElements(), page, size);
        }
        Set<UUID> ids = result.content().stream()
            .map(User::id)
            .collect(Collectors.toSet());
        Set<UUID> totpEnabled = totpStatusPort.findTotpEnabledAmong(ids);
        List<UserAdminView> views = result.content().stream()
            .map(u -> new UserAdminView(
                u.id(), u.username(), u.email(), u.role(),
                u.isActive(), u.createdAt(), totpEnabled.contains(u.id())
            ))
            .toList();
        return new PageResult<>(views, result.totalElements(), page, size);
    }
}
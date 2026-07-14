package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.UserAdminView;
import com.nido.api.shared.model.PageResult;
import com.nido.api.shared.model.SortRequest;

public interface ListUsersUseCase {
    /**
     * @param search optional case-insensitive filter matching username or email
     *               (substring); null or blank means no filtering
     */
    PageResult<UserAdminView> listUsers(int page, int size, SortRequest sort, String search);
}
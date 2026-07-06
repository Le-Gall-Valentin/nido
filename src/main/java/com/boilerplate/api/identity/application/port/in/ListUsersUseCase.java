package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.UserAdminView;
import com.boilerplate.api.shared.model.PageResult;
import com.boilerplate.api.shared.model.SortRequest;

public interface ListUsersUseCase {
    /**
     * @param search optional case-insensitive filter matching username or email
     *               (substring); null or blank means no filtering
     */
    PageResult<UserAdminView> listUsers(int page, int size, SortRequest sort, String search);
}
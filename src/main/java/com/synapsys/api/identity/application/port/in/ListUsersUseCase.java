package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.UserAdminView;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

public interface ListUsersUseCase {
    /**
     * @param search optional case-insensitive filter matching username or email
     *               (substring); null or blank means no filtering
     */
    PageResult<UserAdminView> listUsers(int page, int size, SortRequest sort, String search);
}
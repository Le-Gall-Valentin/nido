package com.synapsys.api.identity.domain.port.out;

import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

public interface UserAdminPort {
    boolean isEmpty();

    /**
     * @param search optional case-insensitive filter matching username or email
     *               (substring); null or blank means no filtering
     */
    PageResult<User> findAll(int page, int size, SortRequest sort, String search);
}
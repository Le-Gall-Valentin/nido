package com.nido.api.space.domain.port.out;

import com.nido.api.shared.model.PageResult;
import com.nido.api.space.domain.model.SpaceAdminView;

public interface SpaceAdminPort {
    PageResult<SpaceAdminView> findAll(int page, int size);
}

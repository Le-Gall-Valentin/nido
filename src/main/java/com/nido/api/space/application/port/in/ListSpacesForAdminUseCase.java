package com.nido.api.space.application.port.in;

import com.nido.api.shared.model.PageResult;
import com.nido.api.space.domain.model.SpaceAdminView;

public interface ListSpacesForAdminUseCase {
    PageResult<SpaceAdminView> list(int page, int size);
}

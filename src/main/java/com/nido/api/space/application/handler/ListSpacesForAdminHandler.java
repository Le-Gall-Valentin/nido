package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.PageResult;
import com.nido.api.space.application.port.in.ListSpacesForAdminUseCase;
import com.nido.api.space.domain.model.SpaceAdminView;
import com.nido.api.space.domain.port.out.SpaceAdminPort;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ListSpacesForAdminHandler implements ListSpacesForAdminUseCase {

    private final SpaceAdminPort spaceAdminPort;

    public ListSpacesForAdminHandler(SpaceAdminPort spaceAdminPort) {
        this.spaceAdminPort = spaceAdminPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SpaceAdminView> list(int page, int size) {
        return spaceAdminPort.findAll(page, size);
    }
}

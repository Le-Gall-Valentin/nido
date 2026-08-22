package com.nido.api.space.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.shared.model.PageResult;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import com.nido.api.space.application.port.in.DeleteEmptySpaceUseCase;
import com.nido.api.space.application.port.in.ListSpacesForAdminUseCase;
import com.nido.api.space.domain.model.SpaceAdminView;
import com.nido.api.space.infrastructure.web.dto.PageResponse;
import com.nido.api.space.infrastructure.web.dto.SpaceAdminItemResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/spaces")
@Validated
@Tag(name = "Administration des contextes", description = "Métadonnées, sans accès au contenu")
public class SpaceAdminController {

    private static final Logger log = LoggerFactory.getLogger(SpaceAdminController.class);

    private final ListSpacesForAdminUseCase listSpacesForAdminUseCase;
    private final DeleteEmptySpaceUseCase deleteEmptySpaceUseCase;

    public SpaceAdminController(ListSpacesForAdminUseCase listSpacesForAdminUseCase,
                                DeleteEmptySpaceUseCase deleteEmptySpaceUseCase) {
        this.listSpacesForAdminUseCase = listSpacesForAdminUseCase;
        this.deleteEmptySpaceUseCase = deleteEmptySpaceUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<SpaceAdminItemResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        // Consultation de métadonnées de contextes : tracée, parce qu'elle sort du périmètre de l'appelant.
        log.info("Platform admin {} listed space metadata (page {})", caller.userId(), page);
        PageResult<SpaceAdminView> result = listSpacesForAdminUseCase.list(page, size);
        return ResponseEntity.ok(new PageResponse<>(
            result.content().stream().map(SpaceAdminItemResponse::from).toList(),
            result.totalElements(), result.page(), result.size()));
    }

    @DeleteMapping("/{spaceId}")
    @RateLimiting(max = 10)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEmpty(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        deleteEmptySpaceUseCase.delete(spaceId, caller.userId());
        return ResponseEntity.noContent().build();
    }
}

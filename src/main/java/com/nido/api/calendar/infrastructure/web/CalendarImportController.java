package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.ImportCalendarUseCase;
import com.nido.api.calendar.infrastructure.web.dto.ImportReportResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * The file travels as the request's own body, typed text/calendar — not as a form: a type no page of another site may
 * send without the browser asking this server first, the second guard client.ts relies on beside SameSite cookies.
 */
@RestController
@RequestMapping("/api/spaces/{spaceId}/calendar/import")
@Tag(name = "Calendrier", description = "Import iCal dans le calendrier d'un contexte")
public class CalendarImportController {

    private static final String TEXT_CALENDAR = "text/calendar";

    private final ImportCalendarUseCase importUseCase;

    public CalendarImportController(ImportCalendarUseCase importUseCase) {
        this.importUseCase = importUseCase;
    }

    @PostMapping(value = "/analysis", consumes = TEXT_CALENDAR)
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImportReportResponse> analyse(
            @PathVariable UUID spaceId, InputStream body,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership)
            throws IOException {
        return ResponseEntity.ok(ImportReportResponse.from(importUseCase.analyse(IcalUpload.read(body), membership)));
    }

    @PostMapping(consumes = TEXT_CALENDAR)
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImportReportResponse> importFile(
            @PathVariable UUID spaceId, InputStream body,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership)
            throws IOException {
        return ResponseEntity.ok(ImportReportResponse.from(importUseCase.importFile(IcalUpload.read(body), membership)));
    }
}

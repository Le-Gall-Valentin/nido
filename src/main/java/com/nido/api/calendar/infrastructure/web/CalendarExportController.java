package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.ExportCalendarUseCase;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.IcalDocument;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/calendar")
@Tag(name = "Calendrier", description = "Export iCal du calendrier d'un contexte")
public class CalendarExportController {

    private final ExportCalendarUseCase exportUseCase;

    public CalendarExportController(ExportCalendarUseCase exportUseCase) {
        this.exportUseCase = exportUseCase;
    }

    @GetMapping("/export.ics")
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> export(
            @PathVariable UUID spaceId,
            // Names rather than the enum: an unknown one is then answered invalid_sources, as an unexportable one is.
            @RequestParam(required = false) List<String> sources,
            @RequestParam(defaultValue = "false") boolean onlyMine,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        IcalDocument document = exportUseCase.export(CalendarSourceType.named(sources), onlyMine, membership);
        return ResponseEntity.ok()
            .contentType(IcalFiles.TEXT_CALENDAR)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(IcalFiles.filename(document.calendarName(), document.day()), StandardCharsets.UTF_8)
                .build().toString())
            .body(document.content());
    }
}

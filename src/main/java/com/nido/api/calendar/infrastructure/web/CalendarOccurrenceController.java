package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.ListCalendarOccurrencesUseCase;
import com.nido.api.calendar.infrastructure.web.dto.CalendarOccurrenceResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/calendar")
@Validated
@Tag(name = "Calendrier", description = "Flux unifié des éléments datés d'un contexte")
public class CalendarOccurrenceController {

    private final ListCalendarOccurrencesUseCase listUseCase;

    public CalendarOccurrenceController(ListCalendarOccurrencesUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    /** One request per window, whatever the number of sources — never one call per source. */
    @GetMapping("/occurrences")
    @RateLimiting(max = 120)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CalendarOccurrenceResponse>> list(
            @PathVariable UUID spaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listUseCase.list(membership, from, to).stream()
            .map(CalendarOccurrenceResponse::from).toList());
    }
}

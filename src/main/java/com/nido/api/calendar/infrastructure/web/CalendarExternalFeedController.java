package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.FollowExternalFeedUseCase;
import com.nido.api.calendar.application.port.in.ListExternalFeedsUseCase;
import com.nido.api.calendar.application.port.in.RefreshExternalFeedUseCase;
import com.nido.api.calendar.application.port.in.RemoveExternalFeedUseCase;
import com.nido.api.calendar.application.port.in.UpdateExternalFeedUseCase;
import com.nido.api.calendar.domain.model.FollowExternalFeedCommand;
import com.nido.api.calendar.domain.model.UpdateExternalFeedCommand;
import com.nido.api.calendar.infrastructure.web.dto.ExternalFeedRequest;
import com.nido.api.calendar.infrastructure.web.dto.ExternalFeedResponse;
import com.nido.api.calendar.infrastructure.web.dto.ExternalFeedUpdateRequest;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/calendar/external-feeds")
@Validated
@Tag(name = "Calendrier", description = "Calendriers externes suivis par un contexte")
public class CalendarExternalFeedController {

    private final FollowExternalFeedUseCase followUseCase;
    private final ListExternalFeedsUseCase listUseCase;
    private final UpdateExternalFeedUseCase updateUseCase;
    private final RemoveExternalFeedUseCase removeUseCase;
    private final RefreshExternalFeedUseCase refreshUseCase;

    public CalendarExternalFeedController(FollowExternalFeedUseCase followUseCase, ListExternalFeedsUseCase listUseCase,
                                          UpdateExternalFeedUseCase updateUseCase, RemoveExternalFeedUseCase removeUseCase,
                                          RefreshExternalFeedUseCase refreshUseCase) {
        this.followUseCase = followUseCase;
        this.listUseCase = listUseCase;
        this.updateUseCase = updateUseCase;
        this.removeUseCase = removeUseCase;
        this.refreshUseCase = refreshUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExternalFeedResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listUseCase.list(membership).stream().map(ExternalFeedResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExternalFeedResponse> follow(
            @PathVariable UUID spaceId, @Valid @RequestBody ExternalFeedRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ExternalFeedResponse.from(followUseCase.follow(
            new FollowExternalFeedCommand(request.url(), request.name(), request.color()), membership)));
    }

    @PatchMapping("/{feedId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExternalFeedResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID feedId, @Valid @RequestBody ExternalFeedUpdateRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.ok(ExternalFeedResponse.from(updateUseCase.update(feedId, new UpdateExternalFeedCommand(request.name(), request.color()), membership)));
    }

    @DeleteMapping("/{feedId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> remove(
            @PathVariable UUID spaceId, @PathVariable UUID feedId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        removeUseCase.remove(feedId, membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{feedId}/refresh")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExternalFeedResponse> refresh(
            @PathVariable UUID spaceId, @PathVariable UUID feedId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.ok(ExternalFeedResponse.from(refreshUseCase.refresh(feedId, membership)));
    }
}

package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.CreateSubscriptionLinkUseCase;
import com.nido.api.calendar.application.port.in.ListSubscriptionLinksUseCase;
import com.nido.api.calendar.application.port.in.RevokeSubscriptionLinkUseCase;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.CreateSubscriptionLinkCommand;
import com.nido.api.calendar.domain.model.CreatedSubscriptionLink;
import com.nido.api.calendar.infrastructure.web.dto.CreatedSubscriptionLinkResponse;
import com.nido.api.calendar.infrastructure.web.dto.SubscriptionLinkRequest;
import com.nido.api.calendar.infrastructure.web.dto.SubscriptionLinkResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/calendar/subscription-links")
@Validated
@Tag(name = "Calendrier", description = "Adresses d'abonnement iCal d'un contexte")
public class CalendarSubscriptionLinkController {

    private final CreateSubscriptionLinkUseCase createUseCase;
    private final ListSubscriptionLinksUseCase listUseCase;
    private final RevokeSubscriptionLinkUseCase revokeUseCase;

    public CalendarSubscriptionLinkController(CreateSubscriptionLinkUseCase createUseCase,
                                              ListSubscriptionLinksUseCase listUseCase,
                                              RevokeSubscriptionLinkUseCase revokeUseCase) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.revokeUseCase = revokeUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SubscriptionLinkResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listUseCase.list(membership).stream().map(SubscriptionLinkResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreatedSubscriptionLinkResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody SubscriptionLinkRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        CreatedSubscriptionLink created = createUseCase.create(new CreateSubscriptionLinkCommand(request.label(),
            request.sources() == null || request.sources().isEmpty()
                ? EnumSet.noneOf(CalendarSourceType.class) : EnumSet.copyOf(request.sources()),
            request.onlyMineOrDefault()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedSubscriptionLinkResponse(
            SubscriptionLinkResponse.from(created.link()), created.url(), created.webcalUrl()));
    }

    @DeleteMapping("/{linkId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID spaceId, @PathVariable UUID linkId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        revokeUseCase.revoke(linkId, membership);
        return ResponseEntity.noContent().build();
    }
}

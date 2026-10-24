package com.nido.api.finance.domain.model;

import java.util.UUID;

/** Who did something in the finances of which space — what every finance notification starts from. */
public record Gesture(UUID spaceId, String spaceName, UUID actorId, String actorName) {
}

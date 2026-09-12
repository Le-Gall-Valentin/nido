package com.nido.api.space.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/**
 * What day it is, for a given space.
 *
 * <p>Exists because the answer is not the server's. The application runs in UTC and the households
 * that use it do not: for the first hours of every morning in Paris, and the last hours of every
 * evening west of Greenwich, the server's date and the user's calendar disagree. Anything that
 * decides what is due, what is late, or which month is on screen has to ask this rather than the
 * clock it happens to be running on.
 *
 * <p>Shaped as an application port for the same reason as
 * {@link GetSpaceEncryptionSaltUseCase}: finance and tasks need a per-space value that space owns,
 * and neither may reach into space's persistence to get it.
 */
public interface GetSpaceTodayUseCase {
    LocalDate today(UUID spaceId);
}

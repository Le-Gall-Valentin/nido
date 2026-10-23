package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.port.out.ImportedUidRepository;
import com.nido.api.shared.annotation.ApplicationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Which UIDs of a file a space holds already: imported before — known by their digest, never stored as written —, or
 * the id of one of its own rows a nido export of that space named ("<uuid>@nido"). What was changed here since is never
 * overwritten by an import.
 */
@ApplicationService
public class KnownImportUids {

    private static final Pattern NIDO_UID = Pattern.compile("^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})@nido$");

    private final ImportedUidRepository importedUids;

    public KnownImportUids(ImportedUidRepository importedUids) {
        this.importedUids = importedUids;
    }

    public Set<String> amongThese(UUID spaceId, Set<String> uids) {
        Map<String, String> hashes = new HashMap<>();
        Map<String, UUID> nidoIds = new HashMap<>();
        for (String uid : uids) {
            hashes.put(uid, CalendarDigests.sha256Hex(uid));
            nidoId(uid).ifPresent(id -> nidoIds.put(uid, id));
        }
        Set<String> known = importedUids.knownHashes(spaceId, List.copyOf(hashes.values()));
        Set<UUID> existing = importedUids.existingIds(spaceId, List.copyOf(nidoIds.values()));
        return uids.stream()
            .filter(uid -> known.contains(hashes.get(uid)) || nidoIds.containsKey(uid) && existing.contains(nidoIds.get(uid)))
            .collect(Collectors.toSet());
    }

    private static Optional<UUID> nidoId(String uid) {
        Matcher matcher = NIDO_UID.matcher(uid);
        return matcher.matches() ? Optional.of(UUID.fromString(matcher.group(1))) : Optional.empty();
    }
}

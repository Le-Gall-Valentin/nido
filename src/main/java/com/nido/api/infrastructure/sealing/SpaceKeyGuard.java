package com.nido.api.infrastructure.sealing;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The key check over every space's data (see {@link ExistingCiphertextCheck}): for each space, the values already
 * encrypted in the sealed columns must open with the key this start was given — at least one of them. A space refuses
 * the start only when it holds encrypted values and none opens: a single damaged value is left to the migration, which
 * names it, rather than taken for a wrong key. A sealed value that decrypts but belongs elsewhere still proves the key.
 * A value of the current format opens with the current key; one of a format before 0.16.0 with the legacy key
 * (LegacySpaceOpener).
 */
@Component
public class SpaceKeyGuard implements ExistingCiphertextCheck {

    /** Values tried per space and column: enough that a few damaged ones cannot pass for a wrong key. */
    static final int SAMPLES_PER_SPACE = 5;

    private final JdbcClient jdbc;
    private final SpaceSealers sealers;
    private final LegacySpaceOpeners legacy;
    private final List<SealedColumns> declared;

    public SpaceKeyGuard(JdbcClient jdbc, SpaceSealers sealers, LegacySpaceOpeners legacy, List<SealedColumns> declared) {
        this.jdbc = jdbc;
        this.sealers = sealers;
        this.legacy = legacy;
        this.declared = declared;
    }

    @Override
    public void verify() {
        Map<UUID, Boolean> proven = new LinkedHashMap<>();
        LegacySpaceOpeners remembered = LegacySpaceOpeners.remembering(legacy);
        for (SealedColumns columns : declared) {
            for (SealedColumn column : columns.columns()) {
                jdbc.sql("SELECT space_id, id, value FROM (SELECT " + column.spaceOf() + " AS space_id, t.id, t."
                        + column.column() + " AS value, row_number() OVER (PARTITION BY " + column.spaceOf() + " ORDER BY t.id) AS n"
                        + " FROM " + column.table() + " t " + column.join() + " WHERE t." + column.column() + " IS NOT NULL) s"
                        + " WHERE n <= " + SAMPLES_PER_SPACE)
                    .query((rs, rowNum) -> new Sample(rs.getObject("space_id", UUID.class), rs.getObject("id", UUID.class),
                        rs.getString("value")))
                    .list()
                    .stream()
                    .filter(sample -> !proven.getOrDefault(sample.spaceId(), false))
                    .forEach(sample -> proven.merge(sample.spaceId(), opens(column, sample, remembered), Boolean::logicalOr));
            }
        }
        proven.forEach((space, opened) -> {
            if (!opened) {
                throw new IllegalStateException("The encryption key does not decrypt the data already encrypted in space "
                    + space + ": nothing was encrypted with it. Start with the key this database was encrypted with.");
            }
        });
    }

    private boolean opens(SealedColumn column, Sample sample, LegacySpaceOpeners legacy) {
        if (SpaceSealer.isSealed(sample.value())) {
            try {
                sealerOf(sample.spaceId()).open(column, sample.id(), sample.value());
                return true;
            } catch (SealedValueRejected rejected) {
                return rejected.reason() != SealedValueRejected.Reason.UNDECRYPTABLE;
            }
        }
        return legacyOf(sample.spaceId(), legacy).decrypts(sample.value());
    }

    private LegacySpaceOpener legacyOf(UUID spaceId, LegacySpaceOpeners legacy) {
        try {
            return legacy.forSpace(spaceId);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not derive the key of space " + spaceId + " ("
                + e.getClass().getSimpleName() + "): its salt may be damaged. Nothing was encrypted.");
        }
    }

    /** A salt that yields no key is not a wrong key: saying so would send the operator the wrong way. */
    private SpaceSealer sealerOf(UUID spaceId) {
        try {
            return sealers.forSpace(spaceId);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not derive the key of space " + spaceId + " ("
                + e.getClass().getSimpleName() + "): its salt may be damaged. Nothing was encrypted.");
        }
    }

    private record Sample(UUID spaceId, UUID id, String value) {}
}

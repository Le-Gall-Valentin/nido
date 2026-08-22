package com.nido.api.space.domain.model;

final class SpaceText {

    private SpaceText() {}

    static String requireName(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > CreateSharedSpaceCommand.NAME_MAX_LENGTH) {
            throw new SpaceException.InvalidSpaceName();
        }
        return trimmed;
    }

    static String normalizeDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > CreateSharedSpaceCommand.DESCRIPTION_MAX_LENGTH) {
            // Refuser plutôt que tronquer : le domaine ne mutile pas silencieusement une saisie.
            throw new SpaceException.InvalidSpaceDescription();
        }
        return trimmed;
    }
}

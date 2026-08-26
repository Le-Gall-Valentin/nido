package com.nido.api.space.domain.model;

final class SpaceText {

    public static final int NAME_MAX_LENGTH = 80;
    public static final int DESCRIPTION_MAX_LENGTH = 280;

    private SpaceText() {}

    /** Le nom est obligatoire : à la création il doit être fourni et non vide. */
    static String requireName(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX_LENGTH) {
            throw new SpaceException.InvalidSpaceName();
        }
        return trimmed;
    }

    /**
     * Nom d'une modification partielle : absent (null) signifie « inchangé ». Le nom
     * étant obligatoire, il ne peut pas être effacé — seulement remplacé.
     */
    static String nameIfPresent(String raw) {
        return raw == null ? null : requireName(raw);
    }

    /** À la création, une description vide ou blanche vaut « pas de description ». */
    static String descriptionOnCreate(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return checkedDescription(trimmed);
    }

    /**
     * À la modification, les trois cas se distinguent : null signifie « inchangé »,
     * une chaîne blanche est un effacement explicite (chaîne vide), sinon on remplace.
     */
    static String descriptionOnUpdate(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return checkedDescription(trimmed);
    }

    private static String checkedDescription(String trimmed) {
        if (trimmed.length() > DESCRIPTION_MAX_LENGTH) {
            // Refuser plutôt que tronquer : le domaine ne mutile pas silencieusement une saisie.
            throw new SpaceException.InvalidSpaceDescription();
        }
        return trimmed;
    }
}

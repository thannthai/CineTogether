package com.thanh.cinetogether.identity.domain;

import java.util.UUID;

public final class DisplayNames {

    private static final int MAX_LENGTH = 80;
    private static final int ID_PREFIX_LENGTH = 6;
    // Glues emoji into one symbol (for example a family), so it must stay allowed inside a name.
    private static final int ZERO_WIDTH_JOINER = 0x200D;

    private DisplayNames() {
    }

    // For a name the user chose: reject a bad one instead of repairing it.
    public static String validated(String name) {
        String clean = cleanOrNull(name);
        if (clean == null) {
            throw new InvalidDisplayNameException("Display name must contain visible characters and no control characters.");
        }
        // VARCHAR(80) counts characters, not UTF-16 units: an emoji is two units but one character.
        if (clean.codePointCount(0, clean.length()) > MAX_LENGTH) {
            throw new InvalidDisplayNameException("Display name must be at most " + MAX_LENGTH + " characters.");
        }
        return clean;
    }

    public static String initialDisplayName(UUID userId, String metadataName, String email) {
        String name = cleanOrNull(metadataName);
        if (name == null && email != null) {
            int at = email.indexOf('@');
            if (at >= 0) {
                name = cleanOrNull(email.substring(0, at));
            }
        }
        if (name == null) {
            name = "Viewer-" + userId.toString().substring(0, ID_PREFIX_LENGTH);
        }
        // Cut on whole characters: cutting by UTF-16 units could split an emoji.
        if (name.codePointCount(0, name.length()) > MAX_LENGTH) {
            name = name.substring(0, name.offsetByCodePoints(0, MAX_LENGTH));
        }
        return name;
    }

    // Null if the value is blank, shows nothing, or has control or invisible format characters (zero-width space,
    // right-to-left override...). strip() alone keeps those, and the database check only trims ASCII whitespace.
    private static String cleanOrNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        boolean hasVisibleCharacter = false;
        for (int i = 0; i < stripped.length(); ) {
            int codePoint = stripped.codePointAt(i);
            i += Character.charCount(codePoint);
            int type = Character.getType(codePoint);
            if (type == Character.CONTROL || (type == Character.FORMAT && codePoint != ZERO_WIDTH_JOINER)) {
                return null;
            }
            if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)
                    && codePoint != ZERO_WIDTH_JOINER) {
                hasVisibleCharacter = true;
            }
        }
        return hasVisibleCharacter ? stripped : null;
    }

}

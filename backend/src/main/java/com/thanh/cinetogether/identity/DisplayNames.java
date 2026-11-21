package com.thanh.cinetogether.identity;

import java.util.UUID;

final class DisplayNames {

    private static final int MAX_LENGTH = 80;
    private static final int ID_PREFIX_LENGTH = 6;

    private DisplayNames() {
    }

    static String initialDisplayName(UUID userId, String metadataName, String email) {
        String name = strippedOrNull(metadataName);
        if (name == null) {
            name = emailLocalPart(email);
        }
        if (name == null) {
            name = "Viewer-" + userId.toString().substring(0, ID_PREFIX_LENGTH);
        }
        return truncate(name);
    }

    private static String emailLocalPart(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at < 0 ? null : strippedOrNull(email.substring(0, at));
    }

    private static String strippedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    // VARCHAR(80) counts characters; cutting by UTF-16 units could split an emoji.
    private static String truncate(String name) {
        if (name.codePointCount(0, name.length()) <= MAX_LENGTH) {
            return name;
        }
        return name.substring(0, name.offsetByCodePoints(0, MAX_LENGTH));
    }

}

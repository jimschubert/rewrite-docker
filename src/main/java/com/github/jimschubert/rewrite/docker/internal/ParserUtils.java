package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.Quoting;

import static com.github.jimschubert.rewrite.docker.internal.ParserConstants.*;

public class ParserUtils {
    public static Docker.Port stringToPorts(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        StringWithPadding stringWithPadding = StringWithPadding.of(s);
        String content = stringWithPadding.content();
        String[] parts = content.split("/");

        if (parts.length == 2) {
            return new Docker.Port(stringWithPadding.prefix(), parts[0], parts[1], true);
        } else if (parts.length == 1) {
            return new Docker.Port(stringWithPadding.prefix(), parts[0], "tcp", false);
        }
        return null;
    }

    public static Docker.KeyArgs stringToKeyArgs(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        StringWithPadding stringWithPadding = StringWithPadding.of(s);
        String content = stringWithPadding.content();

        @SuppressWarnings("RegExpRepeatedSpace")
        String delim = content.contains(EQUAL) ? EQUAL : SPACE;
        String[] parts = content.split(delim, EQUAL.equals(delim) ? 2 : 0);
        String key = parts.length > 0 ? parts[0] : EMPTY;
        String value = parts.length > 1 ? parts[1].trim() : null;
        Quoting q = Quoting.UNQUOTED;

        if (value != null) {
            if (value.startsWith(DOUBLE_QUOTE) && value.endsWith(DOUBLE_QUOTE)) {
                q = Quoting.DOUBLE_QUOTED;
                value = value.substring(1, value.length() - 1);
            } else if (value.startsWith(SINGLE_QUOTE) && value.endsWith(SINGLE_QUOTE)) {
                q = Quoting.SINGLE_QUOTED;
                value = value.substring(1, value.length() - 1);
            }
        }
        return new Docker.KeyArgs(stringWithPadding.prefix(), key, value, EQUAL.equals(delim), q);
    }
}

package com.github.jimschubert.rewrite.docker.internal;

import org.jspecify.annotations.NonNull;

public class StringUtil {
    private StringUtil() {}

    public static @NonNull String trimDoubleQuotes(String text) {
        return trim(text, "\"");
    }

    public static @NonNull String trimSingleQuotes(String text) {
       return trim(text, "'");
    }

    public static @NonNull String trim(String text, String cutset) {
        if (text != null && text.length() > 1 && text.startsWith(cutset) && text.endsWith(cutset)) {
            text = text.substring(1, text.length() - 1);
        }
        return text == null ? "" : text;
    }

    public static @NonNull String trim(String text, String prefix, String suffix) {
        if (text.startsWith(prefix) && text.endsWith(suffix)) {
            text = text.substring(prefix.length(), text.length() - suffix.length());
        }
        return text;
    }
}

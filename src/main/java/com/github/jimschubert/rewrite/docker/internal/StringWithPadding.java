package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Space;

public record StringWithPadding(String content, Space prefix, Space suffix) {
    public static StringWithPadding of(String value) {
        int idx = 0;
        value = value == null ? "" : value;
        for (char c : value.toCharArray()) {
            if (c == ' ' || c == '\t') {
                idx++;
            } else {
                break;
            }
        }

        Space rightPadding = Space.EMPTY;
        Space before = Space.build(value.substring(0, idx));
        value = value.substring(idx);

        idx = value.length() - 1;
        // walk line backwards to find the last non-whitespace character
        for (int i = value.length() - 1; i >= 0; i--) {
            if (value.charAt(i) != ' ' && value.charAt(i) != '\t') {
                // move the pointer to after the current non-whitespace character
                idx = i + 1;
                break;
            }
        }

        if (idx < value.length()) {
            rightPadding = Space.build(value.substring(idx));
            value = value.substring(0, idx);
        }

        return new StringWithPadding(value, before, rightPadding);
    }
}
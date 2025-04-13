/*
 * Copyright (c) 2025 Jim Schubert
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

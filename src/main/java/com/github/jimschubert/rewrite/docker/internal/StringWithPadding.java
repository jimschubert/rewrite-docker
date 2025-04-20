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

import com.github.jimschubert.rewrite.docker.tree.Space;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Accessors(fluent = true)
public class StringWithPadding {
    String content;
    Space prefix;
    Space suffix;

    public static StringWithPadding of(String value) {
        if (value == null || value.isEmpty()) {
            return new StringWithPadding("", Space.EMPTY, Space.EMPTY);
        }

        int idx = 0;
        for (char c : value.toCharArray()) {
            if (c == ' ' || c == '\t') {
                idx++;
            } else {
                break;
            }
        }

        if (idx == value.length()) {
            return new StringWithPadding("", Space.build(value), Space.EMPTY);
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

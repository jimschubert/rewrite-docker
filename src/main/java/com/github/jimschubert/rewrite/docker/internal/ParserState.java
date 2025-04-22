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
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class ParserState {
    private Space prefix = Space.EMPTY;
    private Space rightPadding = Space.EMPTY;
    private char escapeChar = '\\';
    private boolean isContinuation = false;

    public void reset() {
        prefix = Space.EMPTY;
        rightPadding = Space.EMPTY;
        escapeChar = '\\';
        isContinuation = false;
    }

    String getEscapeString() {
        return String.valueOf(escapeChar);
    }

    void appendPrefix(Space prefix) {
        if (prefix != null) {
            this.prefix = prefix.withWhitespace(this.prefix.getWhitespace() + prefix.getWhitespace());
        }
    }

    void resetPrefix() {
        prefix = Space.EMPTY;
    }

    ParserState copy() {
        ParserState copy = new ParserState();
        copy.prefix = this.prefix;
        copy.rightPadding = this.rightPadding;
        copy.escapeChar = this.escapeChar;
        copy.isContinuation = this.isContinuation;
        return copy;
    }
}

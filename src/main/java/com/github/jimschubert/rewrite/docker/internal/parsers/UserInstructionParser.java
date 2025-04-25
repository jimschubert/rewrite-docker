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
package com.github.jimschubert.rewrite.docker.internal.parsers;

import com.github.jimschubert.rewrite.docker.internal.ParserState;
import com.github.jimschubert.rewrite.docker.internal.StringWithPadding;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.Quoting;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import static com.github.jimschubert.rewrite.docker.internal.ParserConstants.USER;

public class UserInstructionParser implements InstructionParser {
    @Override
    public boolean supports(String keyword) {
        return keyword.equalsIgnoreCase(USER);
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        StringWithPadding stringWithPadding = StringWithPadding.of(line);
        String[] parts = stringWithPadding.content().split(":", 2);

        Docker.Literal user;
        Docker.Literal group = null;
        if (parts.length > 1) {
            user = Docker.Literal.build(Quoting.UNQUOTED, stringWithPadding.prefix(), parts[0], Space.EMPTY);
            group = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, parts[1], Space.append(stringWithPadding.suffix(), state.rightPadding()));
        } else {
            user = Docker.Literal.build(Quoting.UNQUOTED, stringWithPadding.prefix(), parts[0], Space.append(stringWithPadding.suffix(), state.rightPadding()));
        }

        return new Docker.User(Tree.randomId(), state.prefix(), user, group, Markers.EMPTY, Space.EMPTY);
    }
}

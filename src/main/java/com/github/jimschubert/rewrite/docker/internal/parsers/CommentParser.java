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
import com.github.jimschubert.rewrite.docker.internal.ParserUtils;
import com.github.jimschubert.rewrite.docker.internal.StringWithPadding;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Quoting;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.util.List;

public class CommentParser implements InstructionParser {
    @Override
    public String instructionName() {
        // a special case where we treat a comment as an instruction
        return "#";
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        StringWithPadding stringWithPadding = StringWithPadding.of(line);

        String lower = stringWithPadding.content().toLowerCase();
        if ((lower.startsWith("syntax=") || lower.startsWith("escape=") || lower.startsWith("check=")) && !lower.contains(" ")) {
            List<DockerRightPadded<Docker.KeyArgs>> args = ParserUtils.parseArgs(line, state);
            DockerRightPadded<Docker.KeyArgs> directive = args.get(0);
            if (directive.getElement().getKey().getText().equalsIgnoreCase("escape")) {
                state.escapeChar(directive.getElement().getValue().getText().charAt(0));
            }

            return new Docker.Directive(Tree.randomId(), state.prefix(), args.get(0), Markers.EMPTY, Space.EMPTY);
        }

        Docker.Literal commentLiteral = ParserUtils.createLiteral(stringWithPadding.content());
        if (commentLiteral == null) {
            // if the comment is empty, we need to create a literal with an empty string
            commentLiteral = Docker.Literal.build(
                    Quoting.UNQUOTED,
                    Space.EMPTY,
                    "",
                    Space.EMPTY);
        }
        commentLiteral = commentLiteral.withPrefix(stringWithPadding.prefix());

        return new Docker.Comment(
                Tree.randomId(),
                state.prefix(),
                commentLiteral.withTrailing(Space.append(commentLiteral.getTrailing(), state.rightPadding())),
                Markers.EMPTY,
                Space.EMPTY
        );
    }
}

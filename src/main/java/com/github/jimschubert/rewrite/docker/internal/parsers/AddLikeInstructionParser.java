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
import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Space;
import lombok.Getter;
import lombok.Value;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static com.github.jimschubert.rewrite.docker.internal.ParserUtils.stringToKeyArgs;

public abstract class AddLikeInstructionParser implements InstructionParser {
    @Value
    @Getter
    static class Elements {
        List<Docker.Option> options;
        List<Docker.Literal> sources;
        Docker.Literal destination;
    }

    protected Elements parseElements(String content, ParserState state) {
        // TODO: COPY allows for heredoc with redirection, but ADD does not
        List<DockerRightPadded<Docker.Literal>> literals = ParserUtils.parseLiterals(content, state);

        List<Docker.Option> options = new ArrayList<>();
        List<Docker.Literal> sources = new ArrayList<>();
        Docker.Literal destination = null;

        // reverse literals iteration
        for (int i = literals.size() - 1; i >= 0; i--) {
            DockerRightPadded<Docker.Literal> literal = literals.get(i);
            // hack: if we have a heredoc, it'll all become the "sources"
            if (state.heredoc() == null && i == literals.size() - 1) {
                // the last literal is the destination
                destination = literal.getElement().withTrailing(Space.append(literal.getElement().getTrailing(), literal.getAfter()));
                continue;
            }

            if (i == 0 && literal.getElement().getPrefix().isEmpty()) {
                literal = literal.map(e -> e.withPrefix(state.prefix()));
            }

            String value = literal.getElement().getText();
            if (value.startsWith("--")) {
                options.add(0, new Docker.Option(
                        Tree.randomId(),
                        literal.getElement().getPrefix(),
                        stringToKeyArgs(literal.getElement().getText()),
                        Markers.EMPTY, literal.getAfter()));
            } else {
                sources.add(0, literal.getElement().withTrailing(Space.append(literal.getElement().getTrailing(), literal.getAfter())));
            }
        }

        return new Elements(options, sources, destination);
    }
}

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
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static com.github.jimschubert.rewrite.docker.internal.ParserUtils.stringToKeyArgs;

public class RunInstructionParser implements InstructionParser {
    @Override
    public boolean supports(String keyword) {
        return keyword.equalsIgnoreCase("RUN");
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        // TODO: Run allows for JSON array syntax (exec form)
        List<DockerRightPadded<Docker.Literal>> literals = ParserUtils.parseLiterals(line, state);
        List<Docker.Option> options = new ArrayList<>();
        List<Docker.Literal> commands = new ArrayList<>();

        boolean doneWithOptions = false;
        for (DockerRightPadded<Docker.Literal> literal : literals) {
            String value = literal.getElement().getText();
            if (!doneWithOptions && (value.startsWith("--mount") || value.startsWith("--network") || value.startsWith("--security"))) {
                options.add(new Docker.Option(
                        Tree.randomId(),
                        literal.getElement().getPrefix(),
                        stringToKeyArgs(literal.getElement().getText()),
                        Markers.EMPTY, literal.getAfter()));
            } else {
                doneWithOptions = true;
                commands.add(literal.getElement().withTrailing(Space.append(literal.getElement().getTrailing(), literal.getAfter())));
            }
        }

        return new Docker.Run(Tree.randomId(), state.prefix(), options, commands, Markers.EMPTY, Space.EMPTY);
    }
}

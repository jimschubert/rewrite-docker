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
import com.github.jimschubert.rewrite.docker.tree.*;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HealthcheckInstructionParser implements InstructionParser {
    @Override
    public String instructionName() {
        return "HEALTHCHECK";
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        StringWithPadding stringWithPadding = StringWithPadding.of(line);
        String content = stringWithPadding.content();
        List<Docker.Literal> commands;
        if (content.equalsIgnoreCase("NONE")) {
            commands = new ArrayList<>();
            Docker.Literal none = Docker.Literal.build(Quoting.UNQUOTED, stringWithPadding.prefix(), content, stringWithPadding.suffix());
            commands.add(none.withTrailing(state.rightPadding()));
            return new Docker.Healthcheck(Tree.randomId(), state.prefix(), Docker.Healthcheck.Type.NONE, null, commands, Markers.EMPTY, Space.EMPTY);
        }

        List<DockerRightPadded<Docker.KeyArgs>> args;
        String[] parts = line.split("CMD", 2);
        if (parts.length > 1) {
            // the first part is the options, but keyargs don't support trailing spaces
            StringWithPadding swp = StringWithPadding.of(parts[0]);
            // HACK if swp is all whitespace, we'll ignore it for now
            if (!swp.content().isEmpty()) {
                args = ParserUtils.parseArgs(swp.prefix().getWhitespace() + swp.content(), state);
            } else {
                args = new ArrayList<>();
            }

            // the second part is the command, prefix it with any keyargs trailing whitespace
            commands = ParserUtils.parseLiterals(Form.SHELL, swp.suffix().getWhitespace() + "CMD" + parts[1], state)
                    .stream().map(d ->
                            d.getElement()
                                    .withTrailing(Space.append(d.getElement().getTrailing(), d.getAfter())))
                    .collect(Collectors.toList());
        } else {
            args = new ArrayList<>();
            commands = ParserUtils.parseLiterals(Form.SHELL, content, state)
                    .stream().map(d ->
                            d.getElement().withTrailing(Space.append(d.getElement().getTrailing(), d.getAfter())))
                    .collect(Collectors.toList());
        }

        return new Docker.Healthcheck(Tree.randomId(), state.prefix(), Docker.Healthcheck.Type.CMD, args, commands, Markers.EMPTY, Space.EMPTY);
    }
}

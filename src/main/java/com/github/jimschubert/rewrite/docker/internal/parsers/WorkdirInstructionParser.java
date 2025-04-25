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

import java.util.List;

public class WorkdirInstructionParser extends CommandLikeInstructionParser {
    @Override
    public String instructionName() {
        return "WORKDIR";
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        Elements elements = parseElements(line, state);

        List<DockerRightPadded<Docker.Literal>> literals = ParserUtils.parseLiterals(elements.getForm(), elements.getContent(), state);

        return new Docker.Workdir(Tree.randomId(), state.prefix(), literals.isEmpty() ? null : literals.get(0).getElement(), Markers.EMPTY, Space.EMPTY);
    }
}

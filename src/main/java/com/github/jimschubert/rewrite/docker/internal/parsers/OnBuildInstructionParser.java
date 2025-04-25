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
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;


public class OnBuildInstructionParser implements InstructionParser {
    private final ParserRegistry registry;

    public OnBuildInstructionParser(ParserRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String instructionName() {
        return "ONBUILD";
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        StringWithPadding stringWithPadding = StringWithPadding.of(line);
        state.prefix(Space.append(state.prefix(), stringWithPadding.prefix()));

        String content = stringWithPadding.content();
        if (stringWithPadding.suffix() != null) {
            state.rightPadding(Space.append(stringWithPadding.suffix(), state.rightPadding()));
        }

        String instruction = peekWord(content);
        content = content.substring(instruction.length()).trim();
        Docker nested = registry.getParserFor(instruction).parse(content, state);

        return new Docker.OnBuild(Tree.randomId(), state.prefix(), nested, state.rightPadding(), Markers.EMPTY, Space.EMPTY);
    }
}

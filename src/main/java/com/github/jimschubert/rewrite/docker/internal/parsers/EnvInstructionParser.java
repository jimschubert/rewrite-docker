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
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

public class EnvInstructionParser implements InstructionParser {
    @Override
    public String instructionName() {
        return "ENV";
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        List<DockerRightPadded<Docker.KeyArgs>> args = ParserUtils.parseArgs(line, state);
        // HACK: generally we have ENV key=value syntax. But we are going to post-process here to work out the alternate syntax.
        //
        // For ENV instruction in Docker, if the first key has no value (no equals sign),
        // then everything that follows is considered the value for that key
        //
        // From Docker's documentation:
        // This syntax does not allow for multiple environment-variables to be set in a single ENV instruction, and
        // can be confusing. For example, the following sets a single environment variable (ONE) with value "TWO= THREE=world":
        //
        // ENV ONE TWO= THREE=world
        List<DockerRightPadded<Docker.KeyArgs>> processedArgs = new ArrayList<>();

        if (!args.isEmpty() && args.get(0).getElement().getValue().getText() == null) {
            Docker.KeyArgs firstKey = args.get(0).getElement();

            // The rest of the args (if any) should be combined as the value for the first key
            if (args.size() > 1) {
                StringBuilder combinedValue = getCombinedValue(args);
                Docker.KeyArgs updatedFirstKey = Docker.KeyArgs.build(firstKey.key(),combinedValue.toString()).withHasEquals(false);
                processedArgs.add(args.get(0).withElement(updatedFirstKey));
            } else {
                // If there are no other args, keep the first one as is
                processedArgs.add(args.get(0));
            }
        } else {
            // Standard key=value syntax, keep the args as they are
            processedArgs.addAll(args);
        }

        return new Docker.Env(Tree.randomId(), state.prefix(), processedArgs, Markers.EMPTY, Space.EMPTY);
    }

    private static @NotNull StringBuilder getCombinedValue(List<DockerRightPadded<Docker.KeyArgs>> args) {
        StringBuilder combinedValue = new StringBuilder();
        for (int i = 1; i < args.size(); i++) {
            Docker.KeyArgs keyArg = args.get(i).getElement();
            if (i > 1) {
                String space = keyArg.getKey().getPrefix().getWhitespace();
                combinedValue.append(!space.isEmpty() ? space : " ");
            }
            combinedValue.append(keyArg.getKey().getText());
            if (keyArg.getValue().getText() != null) {
                if (keyArg.isHasEquals()) {
                    combinedValue.append("=");
                }
                combinedValue.append(keyArg.getValue().getPrefix().getWhitespace());
                combinedValue.append(keyArg.getValue().getText());
            }
        }
        return combinedValue;
    }
}

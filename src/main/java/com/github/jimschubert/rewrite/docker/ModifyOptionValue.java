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
package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.trait.Traits;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.*;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class ModifyOptionValue extends Recipe {
    @Option(displayName = "Match key regex",
            description = "A regular expression to match against text of the key.",
            example = ".*/mount/.*")
    String matchKey;

    @Option(displayName = "Match value regex",
            description = "A regular expression to match against text of the value.",
            example = ".*/var/lib/apt.*",
            required = false)
    String matchValue;

    @Option(displayName = "Replacement value",
            description = "The text to set for the value of the matching option.",
            example = "java-21")
    String replacementText;

    @Option(displayName = "The parent instruction",
            description = "An uppercase Docker instruction to match against exactly. " +
                          "This is not a regex match, but a case-sensitive exact match. " +
                          "Supported values are RUN, ADD, COPY." +
                          "If matchInstructionRegex is set to true, this value may be a regex match against the _entire_ instruction text.",
            example = "RUN",
            required = false)
    String parent;

    @Option(displayName = "Match instruction regex",
            description = "A regular expression to match against the full instruction text.",
            example = ".*https://.+",
            required = false)
    boolean matchInstructionRegex;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Modify option value within a Dockerfile";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Modify option value within a Dockerfile.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        if (parent == null || parent.isEmpty()) {
            return Traits.option(matchKey, matchValue, true)
                    .asVisitor(n -> {
                        Docker.KeyArgs args = n.getTree().getKeyArgs();
                        return n.withArgs(args.withValue(args.getValue().withText(replacementText)))
                                .getTree();
                    });
        }

        return Traits.option(matchKey, matchValue, true)
                .asVisitor(n -> {
                    if (matchInstructionRegex) {
                        Tree tree = n.getCursor().getParent().getValue();
                        String text = tree.printTrimmed(new Cursor(n.getCursor().getParent(), tree));
                        if (!text.matches(parent)) {
                            return n.getTree();
                        }
                    } else {
                        Class<? extends Docker.Instruction> parentTarget;
                        switch (parent.toUpperCase()) {
                            case "RUN":
                                parentTarget = Docker.Run.class;
                                break;
                            case "ADD":
                                parentTarget = Docker.Add.class;
                                break;
                            case "COPY":
                                parentTarget = Docker.Copy.class;
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid parent instruction: " + parent);
                        }

                        if (!parentTarget.isInstance(n.getCursor().getParent().getValue())) {
                            return n.getTree();
                        }
                    }


                    Docker.KeyArgs args = n.getTree().getKeyArgs();
                    return n.withArgs(args.withValue(args.getValue().withText(replacementText)))
                            .getTree();
                });
    }
}

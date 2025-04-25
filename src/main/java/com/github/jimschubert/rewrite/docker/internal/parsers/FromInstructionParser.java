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
import com.github.jimschubert.rewrite.docker.tree.Quoting;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.util.List;

public class FromInstructionParser implements InstructionParser {
    @Override
    public String instructionName() {
        return "FROM";
    }

    @Override
    public Docker.Instruction parse(String line, ParserState state) {
        Docker.Literal platform = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
        Docker.Literal image = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
        Docker.Literal version = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
        Docker.Literal as = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
        Docker.Literal alias = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);

        List<DockerRightPadded<Docker.Literal>> literals = ParserUtils.parseLiterals(line, state);
        if (!literals.isEmpty()) {
            while (!literals.isEmpty()) {
                DockerRightPadded<Docker.Literal> literal = literals.remove(0);
                Docker.Literal elem = literal.getElement();
                String value = literal.getElement().getText();
                Space after = Space.append(elem.getTrailing(), literal.getAfter());
                if (value.startsWith("--platform")) {
                    platform = elem.withTrailing(after);
                } else if (image.getText() != null && "as".equalsIgnoreCase(value)) {
                    as = elem.withTrailing(after);
                } else if ("as".equalsIgnoreCase(as.getText())) {
                    alias = elem.withTrailing(after);
                } else if (image.getText() == null) {
                    image = elem.withTrailing(after);
                    String imageText = literal.getElement().getText();
                    // walk imageText forwards to find the first ':' or '@' to determine the version
                    int idx = 0;
                    for (char c : imageText.toCharArray()) {
                        if (c == ':' || c == '@') {
                            break;
                        }
                        idx++;
                    }

                    if (idx < imageText.length() - 1) {
                        version = ParserUtils.createLiteral(imageText.substring(idx))
                                .withPrefix(Space.EMPTY)
                                .withTrailing(image.getTrailing());
                        imageText = imageText.substring(0, idx);

                        String img = imageText;
                        image = image.withText(img).withTrailing(Space.EMPTY);
                    }
                }
            }
        }

        return new Docker.From(Tree.randomId(), state.prefix(), platform, image, version, as, alias, state.rightPadding(), Markers.EMPTY, Space.EMPTY);
    }
}

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

import com.github.jimschubert.rewrite.docker.trait.DockerLiteral;
import lombok.*;
import org.openrewrite.*;
import org.openrewrite.marker.Marker;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class ModifyLiteral extends Recipe {
    @Option(displayName = "Match text",
            description = "A regular expression to match against text.",
            example = ".*/java-17/.*")
    String matchText;

    @Option(displayName = "Replacement text",
            description = "The replacement text for the matched text. " +
                    "This will replace the full literal text, or a single matching group defined in `matchText`. " +
                    "Be careful as this may result in an invalid Dockerfile.",
            example = "java-21")
    String replacementText;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Modify literal text within a Dockerfile";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Modify literal text within a Dockerfile.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        if (matchText == null || matchText.isEmpty()) {
            return TreeVisitor.noop();
        }

        return DockerLiteral.matcher(matchText)
                .asVisitor(n -> {
                    if (replacementText == null || replacementText.isEmpty()) {
                        return n.withText(replacementText).getTree();
                    }

                    if (n.getTree().getMarkers().findFirst(Modified.class).filter(
                            m -> m.matchText.equals(matchText) && m.replacementText.equals(replacementText)
                        ).isPresent()) {
                        return n.getTree();
                    }

                    String text = n.getText();
                    if (text != null) {
                        Modified m = new Modified(
                                UUID.randomUUID(),
                                matchText,
                                replacementText
                        );

                        Matcher matcher = Pattern.compile(matchText).matcher(n.getText());
                        if (matcher.matches()) {
                            if (matcher.groupCount() > 0) {
                                String newText = text;
                                for (int i = 1; i <= matcher.groupCount(); i++) {
                                    String group = matcher.group(i);
                                    if (group != null) {
                                        newText = newText.replace(group, replacementText);
                                    }
                                }
                                return n.withText(newText).getTree()
                                        .withMarkers(n.getTree().getMarkers().addIfAbsent(m));
                            }

                            return n.withText(matcher.replaceAll(replacementText)).getTree()
                                    .withMarkers(n.getTree().getMarkers().addIfAbsent(m));
                        }
                    }

                    return n.getTree();
                });
    }

    @Value
    static class Modified implements Marker {
        @EqualsAndHashCode.Exclude
        @With
        UUID id;

        String matchText;
        String replacementText;
    }
}

package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.trait.DockerLiteral;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.*;

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

                    String text = n.getText();
                    if (text != null) {
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
                                return n.withText(newText).getTree();
                            }

                            return n.withText(matcher.replaceAll(replacementText)).getTree();
                        }
                    }

                    return n.getTree();
                });
    }
}

package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class RemoveImagePlatform extends Recipe {
    @Option(displayName = "A regular expression to locate an image entry.",
            description = "A regular expression to locate an image entry, defaults to `.+` (all images).",
            example = ".*",
            required = false)
    String matchImage;

    @Override
    public String getDisplayName() {
        return "Remove image platform";
    }

    @Override
    public String getDescription() {
        return "Remove the platform from an image on the FROM instruction.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(){
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext executionContext) {
                String emptyPlatform = "";
                if (".*".equals(matchImage) || ".+".equals(matchImage) || matchImage == null) {
                    from = from.platform(emptyPlatform);
                } else {
                    Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                    if (matcher.matches()) {
                        from = from.platform(emptyPlatform);
                    }
                }
                return super.visitFrom(from, executionContext);
            }
        };
    }
}

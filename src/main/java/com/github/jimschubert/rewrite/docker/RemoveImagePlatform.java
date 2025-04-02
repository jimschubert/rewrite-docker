package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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
                from = super.visitFrom(from, executionContext);
                if (matchImage == null || ".*".equals(matchImage) || ".+".equals(matchImage)) {
                    return from.withPlatform(null);
                }

                Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                if (matcher.matches()) {
                    return from.withPlatform(null);
                }

                return from;
            }
        };
    }
}

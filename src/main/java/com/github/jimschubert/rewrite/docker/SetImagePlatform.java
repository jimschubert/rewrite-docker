package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
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
public class SetImagePlatform extends Recipe {
    @Option(displayName = "Platform",
            description = "The platform to set in the FROM instruction.",
            example = "linux/amd64")
    String platform;

    @Option(displayName = "A regular expression to locate an image entry.",
            description = "A regular expression to locate an image entry, defaults to `.+` (all images).",
            example = ".*",
            required = false)
    String matchImage;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Set the --platform flag in a FROM instruction";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Set the --platform flag in a FROM instruction.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext executionContext) {
                from = super.visitFrom(from, executionContext);
                if (matchImage == null || ".*".equals(matchImage) || ".+".equals(matchImage)) {
                    return from.withPlatform(platform);
                }
                Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                if (matcher.matches()) {
                    return from.withPlatform(platform);
                }
                return from;
            }
        };
    }
}

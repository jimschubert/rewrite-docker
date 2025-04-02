package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class AddOrUpdateDirective extends ScanningRecipe<AddOrUpdateDirective.Scanned> {
    @Option(displayName = "The full directive",
            description = "The full directive without leading comment. One of: syntax, escape, check.",
            example = "check=skip=JSONArgsRecommended;error=true")
    String directive;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Add or update directive";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Add a directive or update an existing directive.";
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        Scanned scanned = new Scanned();
        scanned.hasDirective = false;
        scanned.requiresUpdate = true;
        return scanned;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        if (directive == null) {
            throw new IllegalArgumentException("Directive is required");
        }

        String[] parts = directive.split("=", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Directive must be in the format key=value");
        }

        String key = parts[0];
        if (!key.matches("^(?i)(syntax|escape|check)$")) {
            throw new IllegalArgumentException("Directive must be one of: syntax, escape, check");
        }

        String value = parts[1];

        acc.targetKey = key;
        acc.targetValue = value;

        return new DockerIsoVisitor<>() {
            @Override
            public Docker.Document visitDocument(Docker.Document dockerfile, ExecutionContext ctx) {
                Docker.Document d = super.visitDocument(dockerfile, ctx);
                if (acc.requiresUpdate) {
                    return SearchResult.found(d);
                }
                return d;
            }

            @Override
            public Docker.Directive visitDirective(Docker.Directive directive, ExecutionContext ctx) {
                Docker.Directive d = super.visitDirective(directive, ctx);
                acc.directiveCount = acc.directiveCount + 1;
                if (d.getKey().equalsIgnoreCase(key)) {
                    acc.hasDirective = true;
                    if (d.getValue().trim().equals(value.trim())) {
                        acc.requiresUpdate = false;
                    } else {
                        return SearchResult.found(d);
                    }
                }

                return d;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.requiresUpdate, new DockerIsoVisitor<>() {
            @Override
            public Docker.Document visitDocument(Docker.Document dockerfile, ExecutionContext ctx) {
                dockerfile = super.visitDocument(dockerfile, ctx);
                if (acc.directiveCount == 0 || !acc.hasDirective) {
                    dockerfile.getStages().get(0).getChildren().add(0, Docker.Directive.build(acc.targetKey, acc.targetValue));
                    acc.hasDirective = true;
                }
                return dockerfile;
            }

            @Override
            public Docker.Directive visitDirective(Docker.Directive directive, ExecutionContext ctx) {
                directive = super.visitDirective(directive, ctx);
                if (directive.getKey().equalsIgnoreCase(acc.targetKey)) {
                    return directive.withKey(acc.targetKey).withValue(acc.targetValue);
                }
                return directive;
            }
        });
    }

    public static class Scanned {
        int directiveCount = 0;
        boolean requiresUpdate;
        boolean hasDirective;
        String targetKey;
        String targetValue;
    }
}

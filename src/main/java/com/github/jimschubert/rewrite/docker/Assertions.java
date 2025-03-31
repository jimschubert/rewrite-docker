package com.github.jimschubert.rewrite.docker;


import com.github.jimschubert.rewrite.docker.tree.Dockerfile;
import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before) {
        return Assertions.dockerfile(before, s -> {
        });
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before, Consumer<SourceSpec<Dockerfile.Document>> spec) {
        SourceSpec<Dockerfile.Document> doc = new SourceSpec<>(Dockerfile.Document.class, null, DockerParser.builder(), before, null);
        doc.path("Dockerfile");
        spec.accept(doc);
        return doc;
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before, @Language("dockerfile") @Nullable String after) {
        return dockerfile(before, after, s -> {
        });
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before, @Language("dockerfile") @Nullable String after,
                                   Consumer<SourceSpec<Dockerfile.Document>> spec) {
        SourceSpec<Dockerfile.Document> doc = new SourceSpec<>(Dockerfile.Document.class, null, DockerParser.builder(), before, s -> after);
        doc.path("Dockerfile");
        spec.accept(doc);
        return doc;
    }
}

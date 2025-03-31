package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Dockerfile;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;

import java.util.ArrayList;
import java.util.List;

public class DockerIsoVisitor<T> extends DockerfileVisitor<T> {
    protected Dockerfile.Document visitDockerfile(Dockerfile.Document dockerfile, T ctx) {
        List<Dockerfile.Stage> modifiedStages = new ArrayList<>();

        for (Dockerfile.Stage stage : dockerfile.getStages()) {
            Dockerfile.Stage modifiedStage = (Dockerfile.Stage) stage.acceptDocker(this, ctx);
            modifiedStages.add(modifiedStage);
        }

        return dockerfile.withStages(modifiedStages);
    }

    @Override
    public @Nullable Dockerfile visit(@Nullable Tree tree, T t, Cursor parent) {
        tree = super.visit(tree, t, parent);
        if (tree == null) {
            return null;
        }
        if (tree instanceof Dockerfile.Document) {
            return visitDockerfile((Dockerfile.Document) tree, t);
        }

        return (Dockerfile)tree;
    }
}

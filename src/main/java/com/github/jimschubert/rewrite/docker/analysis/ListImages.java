package com.github.jimschubert.rewrite.docker.analysis;

import com.github.jimschubert.rewrite.docker.DockerIsoVisitor;
import com.github.jimschubert.rewrite.docker.table.ImageUseReport;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ListImages extends ScanningRecipe<List<ImageUseReport.Row>> {
    @EqualsAndHashCode.Exclude
    transient ImageUseReport report = new ImageUseReport(this);

    @Override
    public String getDisplayName() {
        return "List docker images";
    }

    @Override
    public String getDescription() {
        return "A recipe which outputs a data table describing the images referenced in the Dockerfile.";
    }

    @Override
    public List<ImageUseReport.Row> getInitialValue(ExecutionContext ctx) {
        return new ArrayList<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<ImageUseReport.Row> acc) {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.Document visitDocument(Docker.Document dockerfile, ExecutionContext ctx) {
                Path file = dockerfile.getSourcePath();

                if (dockerfile.getStages() != null) {
                    for (Docker.Stage stage : dockerfile.getStages()) {
                        if (stage != null) {
                            for (Docker.Instruction child : stage.getChildren()) {
                                if (child instanceof Docker.From) {
                                    Docker.From from = (Docker.From) child;
                                    String platformSwitch = null;
                                    if (from.getPlatform().getElement() != null && from.getPlatform().getElement().getText() != null) {
                                        platformSwitch = from.getPlatform().getElement().getText().split("=")[1];
                                    }

                                    acc.add(new ImageUseReport.Row(
                                            file.toString(),
                                            from.getImageSpec(),
                                            from.getTag(),
                                            from.getDigest(),
                                            platformSwitch,
                                            from.getAlias().getElement().getText()
                                    ));
                                }
                            }
                        }
                    }
                }

                if (!acc.isEmpty()) {
                    return SearchResult.found(dockerfile);
                }
                return dockerfile;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(List<ImageUseReport.Row> acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
        for (ImageUseReport.Row row : acc) {
            report.insertRow(ctx, row);
        }
        acc.clear();

        return super.generate(Collections.emptyList(), generatedInThisCycle, ctx);
    }
}

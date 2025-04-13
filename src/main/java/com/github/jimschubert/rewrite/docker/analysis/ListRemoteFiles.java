package com.github.jimschubert.rewrite.docker.analysis;

import com.github.jimschubert.rewrite.docker.DockerIsoVisitor;
import com.github.jimschubert.rewrite.docker.table.RemoteFileReport;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import lombok.EqualsAndHashCode;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ListRemoteFiles extends ScanningRecipe<List<RemoteFileReport.Row>> {
    @EqualsAndHashCode.Exclude
    transient RemoteFileReport report = new RemoteFileReport(this);

    @Override
    public String getDisplayName() {
        return "List remote files";
    }

    @Override
    public String getDescription() {
        return "A recipe which outputs a data table describing the remote files referenced in the Dockerfile.";
    }

    @Override
    public List<RemoteFileReport.Row> getInitialValue(ExecutionContext ctx) {
        return new ArrayList<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<RemoteFileReport.Row> acc) {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.Document visitDocument(Docker.Document dockerfile, ExecutionContext ctx) {
                if (dockerfile.getStages() != null) {
                    for (Docker.Stage stage : dockerfile.getStages()) {
                        if (stage != null) {
                            for (Docker child : stage.getChildren()) {
                                if (child instanceof Docker.Add) {
                                    Docker.Add instruction = (Docker.Add) child;
                                    List<DockerRightPadded<Docker.Literal>> urls = instruction.getSources().stream()
                                            .filter(s -> s.getElement().getText().startsWith("http"))
                                            .collect(Collectors.toCollection(ArrayList::new));

                                    if (!urls.isEmpty()) {
                                        urls.forEach(url -> {
                                            acc.add(new RemoteFileReport.Row(
                                                    dockerfile.getSourcePath().toString(),
                                                    url.getElement().getText()
                                            ));
                                        });
                                    }
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
    public Collection<? extends SourceFile> generate(List<RemoteFileReport.Row> acc, ExecutionContext ctx) {
        if (acc.isEmpty()) {
            return Collections.emptyList();
        }

        for (RemoteFileReport.Row row : acc) {
            report.insertRow(ctx, row);
        }

        acc.clear();

        return Collections.emptyList();
    }
}

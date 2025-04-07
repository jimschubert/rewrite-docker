package com.github.jimschubert.rewrite.docker.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class RemoteFileReport extends DataTable<RemoteFileReport.Row> {
    public RemoteFileReport(Recipe recipe) {
        super(recipe,
                "Remote File Report",
                "Contains a report of the remote files used in the Dockerfile.");
    }

    @Value
    @JsonIgnoreType
    public static class Row {
        @Column(displayName = "Dockerfile Path",
                description = "The path to the Dockerfile where the remote file is used.")
        String path;

        @Column(displayName = "Remote File Path",
                description = "The path to the remote file used in the Dockerfile.")
        String url;
    }
}

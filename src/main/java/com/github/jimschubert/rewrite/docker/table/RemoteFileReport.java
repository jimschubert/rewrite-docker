/*
 * Copyright (c) 2025 Jim Schubert
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

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
public class ImageUseReport extends DataTable<ImageUseReport.Row> {

    public ImageUseReport(Recipe recipe) {
        super(recipe,
                "Image Use Report",
                "Contains a report of the images used in the Dockerfile.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Dockerfile Path",
                description = "The path to the Dockerfile where the image is used.")
        String path;

        @Column(displayName = "Image Name",
                description = "The name of the image used in the Dockerfile.")
        String image;

        @Column(displayName = "Image Tag",
                description = "The tag of the image used in the Dockerfile.")
        String tag;

        @Column(displayName = "Image Digest",
                description = "The digest of the image used in the Dockerfile.")
        String digest;

        @Column(displayName = "Image Platform",
                description = "The platform of the image used in the Dockerfile.")
        String platform;

        @Column(displayName = "Image Alias",
                description = "The alias of the image used in the Dockerfile.")
        String alias;

        @Column(displayName = "Stage Number",
                description = "The stage number of the image used in the Dockerfile.")
        Integer stageNumber;
    }
}

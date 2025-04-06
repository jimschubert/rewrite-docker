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
    }
}

package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.table.ImageUseReport;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class ListImagesTest implements RewriteTest {

    @Test
    void listImages() {
        rewriteRun(
                spec -> spec.recipe(new ListImages())
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .dataTableAsCsv(ImageUseReport.class,
                        """
                        path,image,tag,digest,platform,alias
                        Dockerfile,alpine,latest,,,
                        old.dockerfile,alpine,latest,,,build
                        nested/Dockerfile,alpine,latest,,linux/amd64,build
                        nested/Dockerfile,debian,latest,,,
                        """),
                dockerfile(
                        "FROM alpine:latest",
                        spec -> spec.path("Dockerfile")),
                dockerfile(
                        "FROM alpine:latest AS build",
                        spec -> spec.path("old.dockerfile")),
                dockerfile(
                        """
                        FROM --platform=linux/amd64 alpine:latest AS build
                        FROM debian:latest
                        """, spec -> spec.path("nested/Dockerfile"))
        );
    }

}
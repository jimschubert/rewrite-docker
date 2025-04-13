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
package com.github.jimschubert.rewrite.docker.analysis;

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

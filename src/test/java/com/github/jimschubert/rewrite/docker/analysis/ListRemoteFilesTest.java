package com.github.jimschubert.rewrite.docker.analysis;

import com.github.jimschubert.rewrite.docker.table.RemoteFileReport;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class ListRemoteFilesTest implements RewriteTest {

    @Test
    void listRemoteFiles() {
        rewriteRun(
                spec -> spec.recipe(new ListRemoteFiles())
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .dataTableAsCsv(RemoteFileReport.class,
                                """
                                path,url
                                Dockerfile,"https://example.com/file.txt"
                                old.containerfile,"https://example.com/file2.txt"
                                """),
                dockerfile("""
                           FROM alpine:latest
                           ADD https://example.com/file.txt /file.txt
                           """,
                        spec -> spec.path("Dockerfile")),
                dockerfile("""
                           FROM alpine:latest
                           ADD https://example.com/file2.txt /file.txt
                           """,
                        spec -> spec.path("old.containerfile"))
        );
    }
}

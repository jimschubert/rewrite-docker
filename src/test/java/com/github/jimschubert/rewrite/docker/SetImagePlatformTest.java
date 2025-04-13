package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class SetImagePlatformTest implements RewriteTest {
    @Test
    void setPlatformWithDefaultMatchSingleFrom() {
        rewriteRun(
            spec -> spec.recipe(new SetImagePlatform("linux/amd64", null)),
            dockerfile(
                """
                FROM myImage:latest
                """
                ,
                """
                FROM --platform=linux/amd64 myImage:latest
                """
            )
        );
    }

    @Test
    void setPlatformWithDefaultMatchMultipleFrom() {
        rewriteRun(
            spec -> spec.recipe(new SetImagePlatform("linux/amd64", null)),
            dockerfile(
                """
                FROM --platform=linux/arm64 firstImage AS base
                FROM --platform=windows/amd64 secondImage
                """,
                """
                FROM --platform=linux/amd64 firstImage AS base
                FROM --platform=linux/amd64 secondImage
                """
            )
        );
    }

    @Test
    void setPlatformWithCustomMultipleFrom() {
        rewriteRun(
                spec -> spec.recipe(new SetImagePlatform("linux/amd64", ".+dImage")),
                dockerfile(
                        """
                        FROM --platform=linux/arm64 firstImage AS first
                        FROM --platform=windows/amd64 secondImage AS second
                        FROM thirdImage AS third
                        FROM --platform=linux/amd64 fourthImage
                        """,
                        """
                        FROM --platform=linux/arm64 firstImage AS first
                        FROM --platform=linux/amd64 secondImage AS second
                        FROM --platform=linux/amd64 thirdImage AS third
                        FROM --platform=linux/amd64 fourthImage
                        """
                )
        );
    }
}

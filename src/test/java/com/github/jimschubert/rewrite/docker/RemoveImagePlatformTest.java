package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class RemoveImagePlatformTest implements RewriteTest {
    @Test
    void removePlatformWithDefaultMatchSingleFrom() {
        rewriteRun(
                spec -> spec.recipe(new RemoveImagePlatform(null)),
                dockerfile(
                        """
                        FROM --platform=linux/amd64 myImage:latest
                        """,
                        """
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void removePlatformWithDefaultMatchMultipleFrom() {
        rewriteRun(
                spec -> spec.recipe(new RemoveImagePlatform(null)),
                dockerfile(
                        """
                        FROM --platform=linux/arm64 firstImage AS base
                        FROM --platform=windows/amd64 secondImage
                        """,
                        """
                        FROM firstImage AS base
                        FROM secondImage
                        """
                )
        );
    }

    @Test
    void removePlatformWithCustomMatcherMultipleFrom() {
        rewriteRun(
                spec -> spec.recipe(new RemoveImagePlatform(".+t.*?Image")),
                dockerfile(
                        """
                        FROM --platform=linux/arm64 firstImage AS first
                        FROM --platform=windows/amd64 secondImage AS second
                        FROM thirdImage AS third
                        FROM --platform=linux/amd64 fourthImage
                        """,
                        """
                        FROM firstImage AS first
                        FROM --platform=windows/amd64 secondImage AS second
                        FROM thirdImage AS third
                        FROM fourthImage
                        """
                )
        );
    }
}
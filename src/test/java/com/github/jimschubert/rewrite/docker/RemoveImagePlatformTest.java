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

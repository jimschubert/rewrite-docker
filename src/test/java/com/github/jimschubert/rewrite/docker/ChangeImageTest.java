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

class ChangeImageTest implements RewriteTest {
    @Test
    void noChange() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM doNotTouch
                    """
                )
        );
    }

    @Test
    void changeImageName() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM oldImage
                    """,
                    """
                    FROM newImage
                    """
                )
        );
    }

    @Test
    void changeImageNameWithOtherElements() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM --platform=linux/amd64 oldImage AS base
                    FROM --platform=linux/amd64 doNotTouch
                    """,
                    """
                    FROM --platform=linux/amd64 newImage AS base
                    FROM --platform=linux/amd64 doNotTouch
                    """
                )
        );
    }


    @Test
    void changeImageNameWithOtherElementsLowercaseAs() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM --platform=linux/amd64 oldImage as base
                    FROM --platform=linux/amd64 doNotTouch
                    """,
                    """
                    FROM --platform=linux/amd64 newImage as base
                    FROM --platform=linux/amd64 doNotTouch
                    """
                )
        );
    }


    @Test
    void keepVersionWhenSuppliedNull() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("oldImage:.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM oldImage:latest
                    """,
                    """
                    FROM newImage:latest
                    """
                )
        );
    }

    @Test
    void removeVersionWhenSuppliedEmptyString() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("oldImage:.*", "newImage", "", null)),
                dockerfile(
                    """
                    FROM oldImage:latest
                    """
                    ,
                    """
                    FROM newImage
                    """
                )
        );
    }

    @Test
    void removePlatformWhenSuppliedEmptyString() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("oldImage:.*", "newImage", null, "")),
                dockerfile(
                    """
                    FROM --platform=linux/amd64 oldImage:latest
                    """,
                    """
                    FROM newImage:latest
                    """
                )
        );
    }

    @Test
    void testChangeImageWithNonStandardWhitespace() {
        rewriteRun(
                spec -> spec.recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM    --platform=linux/amd64    oldImage:latest as   base  
                    """,
                    """
                    FROM    --platform=linux/amd64    newImage:latest as   base  
                    """
                )
        );
    }
}

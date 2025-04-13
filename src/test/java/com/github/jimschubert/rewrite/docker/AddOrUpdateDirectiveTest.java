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
import org.openrewrite.test.TypeValidation;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class AddOrUpdateDirectiveTest  implements RewriteTest {
    @Test
    void addDirective() {
        rewriteRun(
            spec -> spec.expectedCyclesThatMakeChanges(1)
                    .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                    .recipe(new AddOrUpdateDirective("check=error=true")),
            dockerfile(
                """
                FROM myImage:latest
                """
                ,
                """
                # check=error=true
                FROM myImage:latest
                """
            )
        );
    }

    @Test
    void addDirectiveWhenMultiple() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("syntax=docker/dockerfile:1")),
                dockerfile(
                        """
                        # check=error=true
                        # escape=`
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # check=error=true
                        # escape=`
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirective() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("check=error=true")),
                dockerfile(
                        """
                        # check=error=false
                        FROM myImage:latest
                        """
                        ,
                        """
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirectiveComplex() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("check=skip=JSONArgsRecommended;error=true")),
                dockerfile(
                        """
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # check=skip=JSONArgsRecommended;error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirectiveWhenMultiple() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("escape=|")),
                dockerfile(
                        """
                        # syntax=docker/dockerfile:1
                        # escape=`
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # escape=|
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirectiveWhenCasingMismatch() {
        // According to docker's docs, all of these are the same. But the parser currently doesn't return the preceding spaces or spaces around the equals sign.
        // #directive=value
        // # directive =value
        // #	directive= value
        // # directive = value
        // #	  dIrEcTiVe=value
        // see https://docs.docker.com/reference/dockerfile/#syntax
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("escape=|")),
                dockerfile(
                        """
                        # syntax=docker/dockerfile:1
                        # eScApE=`
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # escape=|
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void addDirectiveWhenMixedWitHComments() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("escape=|")),
                dockerfile(
                        """
                        # syntax=docker/dockerfile:1
                        # unknowndirective=value
                        # escape=`
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # unknowndirective=value
                        # escape=|
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }
}

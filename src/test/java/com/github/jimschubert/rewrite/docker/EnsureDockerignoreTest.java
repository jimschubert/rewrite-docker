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

import java.io.IOException;
import java.nio.file.Paths;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;
import static org.openrewrite.test.SourceSpecs.text;

class EnsureDockerignoreTest implements RewriteTest {
    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.recipe(new EnsureDockerignore());
    }

    @Test
    void dockerfileExistsDockerignoreDoesNot() throws IOException {
        rewriteRun(
                spec -> spec.recipe(new EnsureDockerignore("*.md,*.test")),
                text(
                        //language=dockerignore
                        null,
                        """
                        *.md
                        *.test
                        """,
                        spec -> spec.path(Paths.get(".dockerignore"))
                )
        );
    }

    @Test
    void dockerignoreAlreadyIncludesSomePatterns() throws IOException {
        rewriteRun(
                spec -> spec.recipe(new EnsureDockerignore("*.md,*.test,*.log,*.tmp,*.bak,*.swp,*.DS_Store"))
                        .expectedCyclesThatMakeChanges(1),
                dockerfile(
                        //language=dockerfile
                        """
                        FROM alpine:latest
                        """,
                        spec -> spec.path(Paths.get("Dockerfile"))
                ),
                text(
                        //language=dockerignore
                        """
                        # This is a comment
                        *.test
                        *.DS_Store
                        *.swp
                        """,
                        //language=dockerignore
                        """
                        # This is a comment
                        *.test
                        *.DS_Store
                        *.swp
                        *.bak
                        *.log
                        *.md
                        *.tmp
                        """,
                        spec -> spec.path(Paths.get(".dockerignore"))
                )
        );
    }

    @Test
    void dockerignoreAlreadyIncludesAllPatterns() throws IOException {
        rewriteRun(
                spec -> spec.recipe(new EnsureDockerignore("*.md,*.test,*.log,*.tmp,*.bak,*.swp,*.DS_Store"))
                        .expectedCyclesThatMakeChanges(0),
                text(
                        //language=dockerignore
                        """
                        # This is a comment
                        *.test
                        *.DS_Store
                        *.swp
                        *.bak
                        *.log
                        *.md
                        *.tmp
                        """,
                        spec -> spec.path(Paths.get(".dockerignore"))
                )
        );
    }
}

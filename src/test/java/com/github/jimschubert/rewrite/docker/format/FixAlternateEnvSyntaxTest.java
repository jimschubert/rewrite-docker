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
package com.github.jimschubert.rewrite.docker.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class FixAlternateEnvSyntaxTest implements RewriteTest {
    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.recipe(new FixAlternateEnvSyntax());
    }

    @Test
    void fixEnvSyntax() {
        rewriteRun(
            //language=dockerfile
            dockerfile(
                "ENV key value",
                "ENV key=value"
            )
        );
    }

    @Test
    void fixEnvAlternateSyntaxExample() {
        rewriteRun(
            //language=dockerfile
            dockerfile(
                "ENV ONE TWO= THREE=world",
                "ENV ONE=\"TWO= THREE=world\""
            )
        );
    }
}

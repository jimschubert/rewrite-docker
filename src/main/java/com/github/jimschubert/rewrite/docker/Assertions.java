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


import com.github.jimschubert.rewrite.docker.tree.Docker;
import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before) {
        return Assertions.dockerfile(before, s -> {
        });
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before, Consumer<SourceSpec<Docker.Document>> spec) {
        SourceSpec<Docker.Document> doc = new SourceSpec<>(Docker.Document.class, null, DockerParser.builder(), before, null);
        doc.path("Dockerfile");
        spec.accept(doc);
        return doc;
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before, @Language("dockerfile") @Nullable String after) {
        return dockerfile(before, after, s -> {
        });
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before, @Language("dockerfile") @Nullable String after,
                                   Consumer<SourceSpec<Docker.Document>> spec) {
        SourceSpec<Docker.Document> doc = new SourceSpec<>(Docker.Document.class, null, DockerParser.builder(), before, s -> after);
        doc.path("Dockerfile");
        spec.accept(doc);
        return doc;
    }
}

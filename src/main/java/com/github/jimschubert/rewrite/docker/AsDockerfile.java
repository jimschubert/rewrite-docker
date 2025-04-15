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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.Optional;

/**
 * Treat a file as a Dockerfile. This is useful for files which don't follow a conventional naming standard.
 * <p>
 * For example, if you have a file named "Dockerfile.dev" and you want to treat it as a Dockerfile, you can use this recipe.
 * <p>
 *
 * The idea for this recipe came from a discussion on the OpenRewrite Slack channel, where it was suggested to
 * <a href="https://github.com/openrewrite/rewrite/pull/3993#issuecomment-2227376531">force load via recipe</a>.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class AsDockerfile extends Recipe {
    @Option(displayName = "File pattern",
            description = "A file path glob expression to match from the project root. Blank/null matches all." +
                    "Supports multiple patterns separated by a semicolon `;`.",
            required = false,
            example = ".github/workflows/*.yml")
    String filePattern;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Treat a file as a Dockerfile";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Treat a file as a Dockerfile. This is useful for files which don't follow a conventional naming standard.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        PlainTextVisitor<ExecutionContext> visitor = new PlainTextVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, @NonNull ExecutionContext executionContext) {
                if (tree instanceof PlainText) {
                    PlainText plainText = (PlainText) tree;
                    Optional<SourceFile> maybeDockerfile = DockerParser.builder().build().parse(plainText.print(getCursor())).findFirst();
                    if (maybeDockerfile.isPresent()) {
                        Docker.Document dockerfile = (Docker.Document) maybeDockerfile.get();
                        return dockerfile
                                .withId(plainText.getId())
                                .withCharset(plainText.getCharset())
                                .withSourcePath(plainText.getSourcePath())
                                .withFileAttributes(plainText.getFileAttributes())
                                .withChecksum(plainText.getChecksum())
                                .withCharsetBomMarked(plainText.isCharsetBomMarked())
                                .withMarkers(plainText.getMarkers());
                    }
                }
                return super.visit(tree, executionContext);
            }
        };

        return Preconditions.check(new FindSourceFiles(filePattern), visitor);
    }
}

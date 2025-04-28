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

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.text.PlainText;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class EnsureDockerignore extends ScanningRecipe<EnsureDockerignore.Scanned> {
    private static final String DEFAULT_EXCLUDES = ".env,.log,.tmp,.bak,.swp,.DS_Store,.class,.md,.txt,.adoc,.git,.idea,.vscode,.gradle,.mvn";

    @Option(
            displayName = "Excludes",
            description = "A comma-separated list of patterns to exclude from the .dockerignore file.",
            example = ".env,*.log,*.tmp,*.bak,*.swp,*.DS_Store,*.class,*.md,*.txt,*.adoc,.git,.idea/,.vscode/,.gradle/,.mvn/",
            required = false
    )
    String excludes;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Ensure .dockerignore file exists";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Ensure that a .dockerignore file exists in the project root with (at a minimum) the recommended excludes patterns.";
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext, Cursor parent) {
                if (tree == null) {
                    return tree;
                }

                SourceFile file = (SourceFile) tree;
                if (file.getSourcePath().endsWith("Dockerfile") || file.getSourcePath().endsWith(".dockerignore")) {
                    // if dockerfile, and .dockerignore is never loaded as plain text, this will retrieve from the file
                    evaluatePath(acc, file.getSourcePath());

                    // but if it's a .dockerignore, we can also read in the content, which will include any recipe updates to this point
                    if (file.getSourcePath().endsWith(".dockerignore")) {
                        evaluateContent(acc, file.printAll());
                    }

                    acc.found = true;
                }
                return file;
            }
        };
    }


    @Override
    public Collection<? extends SourceFile> generate(Scanned acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
        Collection<? extends SourceFile> result = Collections.emptyList();
        if (!acc.found) {
            Path target = Paths.get(".dockerignore");
            if (generatedInThisCycle.stream().noneMatch(f -> f.getSourcePath().equals(target))) {
                String toAppend = remainingExcludes(getExcludes(), acc.existingExcludes);
                if (toAppend.isEmpty()) {
                    return Collections.emptyList();
                }

                return Collections.singletonList(PlainText.builder()
                        .text(toAppend)
                        .sourcePath(target)
                        .build());
            }
        }

        return result;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.found, new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree != null) {
                    SourceFile file = (SourceFile) tree;
                    if (file.getSourcePath().endsWith(".dockerignore")) {
                        // evaluate the file to see if it exists and if it has the excludes we need

                        PlainText plainText = (PlainText) file;
                        String original = plainText.printAll();
                        if (!original.endsWith("\n")) {
                            original = original + "\n";
                        }
                        evaluateContent(acc, original);
                        String append = remainingExcludes(getExcludes(), acc.existingExcludes);

                        return append.isEmpty() ?
                                tree :
                                plainText.withText(original + append);
                    }
                    return file;
                }
                return null;
            }
        });
    }

    private String remainingExcludes(String excludes, Set<String> existingExcludes) {
        if (excludes == null || excludes.isEmpty()) {
            return "";
        }
        Set<String> toBeExcluded = new HashSet<>(List.of(excludes.split(",")));
        toBeExcluded.removeAll(existingExcludes);
        return toBeExcluded.stream()
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .sorted()
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String getExcludes() {
        if (excludes == null || excludes.isEmpty()) {
            return DEFAULT_EXCLUDES;
        }
        return excludes;
    }

    private void evaluatePath(Scanned acc, Path path) {
        // path is the Dockerfile path or .dockerignore path,
        // get the root path of the Dockerfile to construct the .dockerignore path
        // and check if it exists.
        if (path.toFile().exists() && path.getFileName().endsWith(".dockerignore")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    acc.existingExcludes.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void evaluateContent(Scanned acc, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            acc.existingExcludes.add(line);
        }
    }

    public static class Scanned {
        boolean found;
        Set<String> existingExcludes = new HashSet<>();
    }
}

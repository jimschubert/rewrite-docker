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

import com.github.jimschubert.rewrite.docker.internal.DockerfileParser;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.stream.Stream;

public class DockerParser implements Parser {
    @Override
    public Stream<SourceFile> parseInputs(Iterable<Parser.Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sources).map(input -> {
            parsingListener.startedParsing(input);
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                DockerfileParser parser = new DockerfileParser();

                Docker.Document document = parser.parse(is)
                        .withFileAttributes(input.getFileAttributes())
                        .withSourcePath(input.getPath());

                parsingListener.parsed(input, document);

                return requirePrintEqualsInput(
                        document.withCharset(is.getCharset()),
                        input,
                        relativeTo,
                        ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.toString();
        return fileName.endsWith(".dockerfile") || fileName.equalsIgnoreCase("dockerfile") ||
               fileName.endsWith(".containerfile") || fileName.equalsIgnoreCase("containerfile");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("Dockerfile");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        public Builder() {
            super(Docker.Document.class);
        }

        @Override
        public DockerParser build() {
            return new DockerParser();
        }

        @Override
        public String getDslName() {
            return "dockerfile";
        }
    }
}

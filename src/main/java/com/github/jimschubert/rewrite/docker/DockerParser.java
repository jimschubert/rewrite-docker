package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.docker.parser.DockerfileParser;
import com.github.jimschubert.rewrite.docker.internal.DockerfileParserVisitor;
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
        return acceptedInputs(sources).map(input ->{
            parsingListener.startedParsing(input);
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                DockerfileParser parser = new DockerfileParser(false);

                Docker.Document document = new DockerfileParserVisitor(
                        input.getRelativePath(relativeTo),
                        input.getFileAttributes(),
                        input.getSource(ctx)
                ).visit(parser.parseDockerfile(is));

                parsingListener.parsed(input, document);

                return requirePrintEqualsInput(document, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.toString();
        return fileName.endsWith(".dockerfile") || fileName.equalsIgnoreCase("dockerfile");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return null;
    }

    public static Builder builder() { return new Builder(); }

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

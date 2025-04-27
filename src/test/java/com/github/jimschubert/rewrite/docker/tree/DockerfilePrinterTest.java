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
package com.github.jimschubert.rewrite.docker.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DockerfilePrinterTest {

    @Test
    void visitFrom() {
        Docker.Document doc = Docker.Document.build(
                Docker.From.build("alpine:latest").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "FROM alpine:latest";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitFromLowercased() {
        Docker.Document doc = Docker.Document.build(
                Docker.From.build("alpine:latest")
                        .withEol(Space.EMPTY)
                        .withMarkers(Markers.build(List.of(new InstructionName(Tree.randomId(), "from"))))
        ).withEof(Space.EMPTY);

        String expected = "from alpine:latest";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitFromIgnoreInvalidInstructionName() {
        Docker.Document doc = Docker.Document.build(
                Docker.From.build("alpine:latest")
                        .withEol(Space.EMPTY)
                        .withMarkers(Markers.build(List.of(new InstructionName(Tree.randomId(), "apples"))))
        ).withEof(Space.EMPTY);

        String expected = "FROM alpine:latest";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitFromFull() {
        Docker.Document doc = Docker.Document.build(
                Docker.From.build("alpine:latest")
                        .alias("build")
                        .platform("linux/amd64").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "FROM --platform=linux/amd64 alpine:latest AS build";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitComment() {
        Docker.Document doc = Docker.Document.build(
                Docker.Comment.build("This is a comment").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "# This is a comment";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitMultilineComment() {
        Docker.Document doc = Docker.Document.build(
                Docker.Comment.build("This is a comment\nwith multiple lines").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "# This is a comment\n# with multiple lines";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitDirective() {
        Docker.Document doc = Docker.Document.build(
                Docker.Directive.build("escape", "'").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "# escape='";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitRun() {
        Docker.Document doc = Docker.Document.build(
                Docker.Run.build("echo Hello World").withEol(Space.NEWLINE),
                Docker.Run.build("echo Goodbye World").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "RUN echo Hello World\nRUN echo Goodbye World";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitRunWithLeftAlignedHeredoc() {
        Docker.Document doc = Docker.Document.build(
                Docker.Run.build("""
                                 <<EOF
                                 apt-get update
                                 apt-get upgrade -y
                                 apt-get install -y ...
                                 EOF
                                 """).withEol(Space.EMPTY)
        );

        String expected = """
                          RUN <<EOF
                          apt-get update
                          apt-get upgrade -y
                          apt-get install -y ...
                          EOF
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitRunWithIndentedHeredoc() {
        Docker.Document doc = Docker.Document.build(
                Docker.Run.build("""
                                 python3 <<EOF
                                 with open("/hello", "w") as f:
                                     print("Hello", file=f)
                                     print("World", file=f)
                                 EOF
                                 """).withEol(Space.EMPTY)
        );

        String expected = """
                          RUN python3 <<EOF
                          with open("/hello", "w") as f:
                              print("Hello", file=f)
                              print("World", file=f)
                          EOF
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitLabelUnquoted() {
        Docker.Document doc = Docker.Document.build(
                Docker.Label.build("version", "1.0").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "LABEL version=1.0";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitLabelQuoted() {
        Docker.Document doc = Docker.Document.build(
                Docker.Label.build("version", "\"1.0\"").withEol(Space.EMPTY)
        ).withEof(Space.EMPTY);

        String expected = "LABEL version=\"1.0\"";
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitLabelMixedQuotes() {
        Docker.Document doc = Docker.Document.build(
                Docker.Label.build("version", "1.0"),
                Docker.Label.build("author", "\"Jim\""),
                Docker.Label.build("description", "'A simple Dockerfile'")
        ).withEof(Space.EMPTY);

        String expected = """
                          LABEL version=1.0
                          LABEL author="Jim"
                          LABEL description='A simple Dockerfile'
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitLabelMultiplePerLine() {
        Docker.Document doc = Docker.Document.build(
                Docker.Label.build(
                        Docker.KeyArgs.build("version", "1.0"),
                        Docker.KeyArgs.build("author", "\"Jim\""),
                        Docker.KeyArgs.build("description", "'A simple Dockerfile'")
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          LABEL version=1.0 author="Jim" description='A simple Dockerfile'
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitMaintainer() {
        Docker.Document doc = Docker.Document.build(
                Docker.Maintainer.build("Jim S <jim@example.com>")
        ).withEof(Space.EMPTY);

        String expected = """
                          MAINTAINER Jim S <jim@example.com>
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitExpose() {
        Docker.Document doc = Docker.Document.build(
                Docker.Expose.build("8080", "8081")
        ).withEof(Space.EMPTY);

        String expected = """
                          EXPOSE 8080 8081
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitExposeWithMixedPortsProvided() {
        Docker.Document doc = Docker.Document.build(
                Docker.Expose.build("8080", "8081/udp", "8082/tcp")
        ).withEof(Space.EMPTY);

        String expected = """
                          EXPOSE 8080 8081/udp 8082/tcp
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitEnvMultiline() {
        Docker.Document doc = Docker.Document.build(
                Docker.Env.build("ENV1", "value1"),
                Docker.Env.build("ENV2", "value2")
        ).withEof(Space.EMPTY);

        String expected = """
                          ENV ENV1=value1
                          ENV ENV2=value2
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitEnvSingleLine() {
        Docker.Document doc = Docker.Document.build(
                Docker.Env.build(
                        Docker.KeyArgs.build("ENV1", "value1"),
                        Docker.KeyArgs.build("ENV2", "value2")
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          ENV ENV1=value1 ENV2=value2
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitEnvSingleLineDeprecatedSyntaxMixed() {
        Docker.Document doc = Docker.Document.build(
                Docker.Env.build(
                        Docker.KeyArgs.build("ENV1", "value1"),
                        Docker.KeyArgs.build("ENV2", "\"value2\"").withHasEquals(false),
                        Docker.KeyArgs.build("ENV3", "${ENV1}"),
                        Docker.KeyArgs.build("ENV4", "${ENV2}").withHasEquals(false)
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          ENV ENV1=value1 ENV2 "value2" ENV3=${ENV1} ENV4 ${ENV2}
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitAddComplex() {
        Docker.Document doc = Docker.Document.build(
                new Docker.Add(
                        Tree.randomId(),
                        Space.EMPTY,
                        List.of(
                            Docker.Option.build("--keep-git-dir", "false")
                        ),
                        List.of(
                            Docker.Literal.build("https://example.com/archive.zip").withPrefix(Space.build(" "))
                        ),
                            Docker.Literal.build("/usr/src/things/").withPrefix(Space.build(" ")
                        ),
                        Markers.EMPTY,
                        Space.NEWLINE
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          ADD --keep-git-dir=false https://example.com/archive.zip /usr/src/things/
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitCopy() {
        Docker.Document doc = Docker.Document.build(
                new Docker.Copy(
                        Tree.randomId(),
                        Space.EMPTY,
                        List.of(
                            Docker.Option.build("--chown", "user:group")
                        ),
                        List.of(
                            Docker.Literal.build("src").withPrefix(Space.build(" "))
                        ),
                        Docker.Literal.build("/dest").withPrefix(Space.build(" ")),
                        Markers.EMPTY,
                        Space.NEWLINE
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          COPY --chown=user:group src /dest
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitEntrypointExecForm() {
        Docker.Document doc = Docker.Document.build(
                Docker.Entrypoint.build("python3", "app.py")
        ).withEof(Space.EMPTY);

        String expected = """
                          ENTRYPOINT ["python3","app.py"]
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitEntrypointShellForm() {
        Docker.Document doc = Docker.Document.build(
                Docker.Entrypoint.build(Form.SHELL,"exec", "top", "-b")
        ).withEof(Space.EMPTY);

        String expected = """
                          ENTRYPOINT exec top -b
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitVolumeExec() {
        Docker.Document doc = Docker.Document.build(
                Docker.Volume.build("/data")
        ).withEof(Space.EMPTY);

        String expected = """
                          VOLUME ["/data"]
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitVolumeShell() {
        Docker.Document doc = Docker.Document.build(
                Docker.Volume.build(Form.SHELL, "/data")
        ).withEof(Space.EMPTY);

        String expected = """
                          VOLUME /data
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitUserSimple() {
        Docker.Document doc = Docker.Document.build(
                Docker.User.build("user")
        ).withEof(Space.EMPTY);

        String expected = """
                          USER user
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitWorkDir() {
        Docker.Document doc = Docker.Document.build(
                Docker.Workdir.build("/app")
        ).withEof(Space.EMPTY);

        String expected = """
                          WORKDIR /app
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitOnBuild() {
        Docker.Document doc = Docker.Document.build(
                Docker.OnBuild.build(
                        Docker.Run.build("echo Hello World").withEol(Space.EMPTY)
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          ONBUILD RUN echo Hello World
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitStopSignal() {
        Docker.Document doc = Docker.Document.build(
                Docker.StopSignal.build("SIGTERM")
        ).withEof(Space.EMPTY);

        String expected = """
                          STOPSIGNAL SIGTERM
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitStopSignalNumerical() {
        Docker.Document doc = Docker.Document.build(
                Docker.StopSignal.build("15")
        ).withEof(Space.EMPTY);

        String expected = """
                          STOPSIGNAL 15
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitHealthCheck() {
        Docker.Document doc = Docker.Document.build(

                new Docker.Healthcheck(
                        Tree.randomId(),
                        Space.EMPTY,
                        Docker.Healthcheck.Type.CMD,
                        List.of(
                                DockerRightPadded.build(
                                    Docker.KeyArgs.build("--interval", "30s")
                                ),
                                DockerRightPadded.build(
                                    Docker.KeyArgs.build("--timeout", "10s")
                                ),
                                DockerRightPadded.build(
                                    Docker.KeyArgs.build("--retries", "3")
                                )
                        ),
                        List.of(
                            Docker.Literal.build("curl -f http://localhost/ || exit 1").withPrefix(Space.build(" "))
                        ),
                        Markers.EMPTY,
                        Space.NEWLINE
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          HEALTHCHECK --interval=30s --timeout=10s --retries=3 CMD curl -f http://localhost/ || exit 1
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitHealthCheckNone() {
        Docker.Document doc = Docker.Document.build(
                new Docker.Healthcheck(
                        Tree.randomId(),
                        Space.EMPTY,
                        Docker.Healthcheck.Type.NONE,
                        List.of(),
                        List.of(),
                        Markers.EMPTY,
                        Space.NEWLINE
                )
        ).withEof(Space.EMPTY);

        String expected = """
                          HEALTHCHECK NONE
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitShell() {
        Docker.Document doc = Docker.Document.build(
                Docker.Shell.build("sh", "-c").withEol(Space.NEWLINE)
        ).withEof(Space.EMPTY);

        String expected = """
                          SHELL ["sh","-c"]
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitCmdShellForm() {
        Docker.Document doc = Docker.Document.build(
                Docker.Cmd.build(Form.SHELL, "echo Hello World")
        ).withEof(Space.EMPTY);

        String expected = """
                          CMD echo Hello World
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitCmdExecForm() {
        Docker.Document doc = Docker.Document.build(
                Docker.Cmd.build(Form.EXEC, "echo", "Hello World")
        ).withEof(Space.EMPTY);

        String expected = """
                          CMD ["echo","Hello World"]
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitArg() {
        Docker.Document doc = Docker.Document.build(
                Docker.Arg.build("MY_ARG", "default_value")
        ).withEof(Space.EMPTY);

        String expected = """
                          ARG MY_ARG=default_value
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }

    @Test
    void visitArgNoValue() {
        Docker.Document doc = Docker.Document.build(
                Docker.Arg.build("MY_ARG", null)
        ).withEof(Space.EMPTY);

        String expected = """
                          ARG MY_ARG
                          """;
        assertEquals(expected, doc.print(new Cursor(null, new DockerfilePrinter<Integer>())));
    }
}

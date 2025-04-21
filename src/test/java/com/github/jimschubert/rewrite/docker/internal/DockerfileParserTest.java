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
package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.jimschubert.rewrite.docker.internal.DockerParserHelpers.*;
import static com.github.jimschubert.rewrite.docker.internal.DockerParserHelpers.assertRightPaddedLiteral;
import static org.junit.jupiter.api.Assertions.*;

class DockerfileParserTest {
    @Test
    void testCommentSingleWithTrailingSpace() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("# This is a comment\t\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Comment comment = (Docker.Comment) stage.getChildren().get(0);
        assertComment(comment, " ", "This is a comment", "\t\t");
    }

    @Test
    void testRetainCarriageReturn() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("# This is a comment\t\t\r\n".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Comment comment = (Docker.Comment) stage.getChildren().get(0);
        assertComment(comment, " ", "This is a comment", "\t\t");
        assertEquals(printableWhiteSpace("\r\n"), printableWhiteSpace(doc.getEof().getWhitespace()));
    }

    @Test
    void testDirective() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("#    syntax=docker/dockerfile:1".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Directive directive = (Docker.Directive) stage.getChildren().get(0);
        assertDirective(directive, "    ", "syntax", true, "docker/dockerfile:1", "");
    }

    @Test
    void testArgNoAssignment() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ARG foo".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Arg arg = (Docker.Arg) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, arg.getPrefix());
        List<DockerRightPadded<Docker.KeyArgs>> args = arg.getArgs();

        assertRightPaddedArg(args.get(0), Quoting.UNQUOTED, " ", "foo", false, null, "");
    }

    @Test
    void testArgComplex() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ARG foo=bar baz MY_VAR OTHER_VAR=\"some default\" \t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Arg arg = (Docker.Arg) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, arg.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = arg.getArgs();
        assertEquals(4, args.size());

        assertRightPaddedArg(args.get(0), Quoting.UNQUOTED, " ", "foo", true, "bar", "");
        assertRightPaddedArg(args.get(1), Quoting.UNQUOTED, " ", "baz", false, null, "");
        assertRightPaddedArg(args.get(2), Quoting.UNQUOTED, " ", "MY_VAR", false, null, "");
        assertRightPaddedArg(args.get(3), Quoting.DOUBLE_QUOTED, " ", "OTHER_VAR", true, "some default", " \t");
    }

    @Test
    void testArgMultiline() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ARG foo=bar baz MY_VAR \\\nOTHER_VAR=\"some default\" \t\\\n\t\tLAST".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Arg arg = (Docker.Arg) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, arg.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = arg.getArgs();
        assertEquals(5, args.size());

        assertRightPaddedArg(args.get(0), Quoting.UNQUOTED, " ", "foo", true, "bar", "");
        assertRightPaddedArg(args.get(1), Quoting.UNQUOTED, " ", "baz", false, null, "");
        assertRightPaddedArg(args.get(2), Quoting.UNQUOTED, " ", "MY_VAR", false, null, " \\\n");
        assertRightPaddedArg(args.get(3), Quoting.DOUBLE_QUOTED, "", "OTHER_VAR", true, "some default", " \t\\\n");
        assertRightPaddedArg(args.get(4), Quoting.UNQUOTED, "\t\t", "LAST", false, null, "");
    }

    @Test
    void testCmdComplexExecForm() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("CMD [ \"echo\", \"Hello World\" ]   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(2, args.size());

        assertLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.DOUBLE_QUOTED, " ", "Hello World", " ");

        assertEquals(" ", cmd.getExecFormPrefix().getWhitespace());
        assertEquals("   ", cmd.getExecFormSuffix().getWhitespace());
    }

    @Test
    void testCmdShellForm() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("CMD echo Hello World   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(3, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "   ");
    }

    @Test
    void testCmdShellFormWithQuotes() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("CMD \"echo Hello World\"   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(1, args.size());

        assertLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo Hello World", "   ");
        assertEquals("", cmd.getExecFormSuffix().getWhitespace());
        assertEquals("", cmd.getExecFormPrefix().getWhitespace());
    }

    @Test
    void testCmdShellWithoutQuotes() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("CMD echo Hello World   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(3, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "   ");
    }

    @Test
    void testEntrypointComplexExecForm() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT [ \"echo\", \"Hello World\" ]   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Entrypoint entrypoint = (Docker.Entrypoint) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, entrypoint.getPrefix());

        List<Docker.Literal> args = entrypoint.getCommands();
        assertEquals(2, args.size());

        assertLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.DOUBLE_QUOTED, " ", "Hello World", " ");

        assertEquals("   ", entrypoint.getExecFormSuffix().getWhitespace());
    }

    @Test
    void testEntrypointShellForm() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT echo Hello World   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Entrypoint entrypoint = (Docker.Entrypoint) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, entrypoint.getPrefix());

        List<Docker.Literal> args = entrypoint.getCommands();
        assertEquals(3, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "   ");
    }

    @Test
    void testEntrypointShellFormWithQuotes() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT \"echo Hello World\"   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Entrypoint entrypoint = (Docker.Entrypoint) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, entrypoint.getPrefix());

        List<Docker.Literal> args = entrypoint.getCommands();
        assertEquals(1, args.size());

        assertLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo Hello World", "   ");
    }

    @Test
    void testEnvComplex() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENV MY_NAME=\"John Doe\" MY_DOG=Rex\\ The\\ Dog MY_CAT=fluffy".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Env env = (Docker.Env) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, env.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = env.getArgs();
        assertEquals(3, args.size());

        assertRightPaddedArg(args.get(0), Quoting.DOUBLE_QUOTED, " ", "MY_NAME", true, "John Doe", "");
        assertRightPaddedArg(args.get(2), Quoting.UNQUOTED, " ", "MY_CAT", true, "fluffy", "");
        assertRightPaddedArg(args.get(1), Quoting.UNQUOTED, " ", "MY_DOG", true, "Rex The Dog", "");
    }

    @Test
    void testEnvMultiline() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENV MY_NAME=\"John Doe\" MY_DOG=Rex\\ The\\ Dog \\\nMY_CAT=fluffy ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Env env = (Docker.Env) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, env.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = env.getArgs();
        assertEquals(3, args.size());

        assertRightPaddedArg(args.get(0), Quoting.DOUBLE_QUOTED, " ", "MY_NAME", true, "John Doe", "");
        assertRightPaddedArg(args.get(1), Quoting.UNQUOTED, " ", "MY_DOG", true, "Rex The Dog", " \\\n");
        assertRightPaddedArg(args.get(2), Quoting.UNQUOTED, "", "MY_CAT", true, "fluffy", " ");
    }

    @Test
    void testExposeSingle() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("EXPOSE 8080".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Expose expose = (Docker.Expose) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, expose.getPrefix());

        List<DockerRightPadded<Docker.Port>> ports = expose.getPorts();
        assertEquals(1, ports.size());
        assertEquals("8080", ports.get(0).getElement().getPort());
        assertEquals(" ", ports.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("", ports.get(0).getAfter().getWhitespace());
        assertEquals("tcp", ports.get(0).getElement().getProtocol());
    }

    @Test
    void testExposeMultiple() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("EXPOSE 8080 8081/udp 9999".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Expose expose = (Docker.Expose) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, expose.getPrefix());

        List<DockerRightPadded<Docker.Port>> ports = expose.getPorts();
        assertEquals(3, ports.size());
        assertEquals("8080", ports.get(0).getElement().getPort());
        assertEquals(" ", ports.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("", ports.get(0).getAfter().getWhitespace());
        assertEquals("tcp", ports.get(0).getElement().getProtocol());

        assertEquals("8081", ports.get(1).getElement().getPort());
        assertEquals(" ", ports.get(1).getElement().getPrefix().getWhitespace());
        assertEquals("", ports.get(1).getAfter().getWhitespace());
        assertEquals("udp", ports.get(1).getElement().getProtocol());

        assertEquals("9999", ports.get(2).getElement().getPort());
        assertEquals(" ", ports.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("", ports.get(2).getAfter().getWhitespace());
        assertEquals("tcp", ports.get(2).getElement().getProtocol());
    }

    @Test
    void testExposeMultiline() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("EXPOSE 8080 \\\n\t\t8081/udp \\\n9999".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Expose expose = (Docker.Expose) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, expose.getPrefix());

        List<DockerRightPadded<Docker.Port>> ports = expose.getPorts();
        assertEquals(3, ports.size());
        assertEquals("8080", ports.get(0).getElement().getPort());
        assertEquals(" ", ports.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n", ports.get(0).getAfter().getWhitespace());
        assertEquals("tcp", ports.get(0).getElement().getProtocol());

        assertEquals("8081", ports.get(1).getElement().getPort());
        assertEquals("\t\t", ports.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n", ports.get(1).getAfter().getWhitespace());
        assertEquals("udp", ports.get(1).getElement().getProtocol());

        assertEquals("9999", ports.get(2).getElement().getPort());
        assertEquals("", ports.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("", ports.get(2).getAfter().getWhitespace());
        assertEquals("tcp", ports.get(2).getElement().getProtocol());
    }

    @Test
    void testFrom() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("FROM alpine:latest".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.From from = (Docker.From) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, from.getPrefix());

        assertLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "");
        assertEquals("latest", from.getTag());
        assertEquals("", from.getImage().getTrailing().getWhitespace());
    }

    @Test
    void testFullFrom() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("FROM --platform=linux/arm64 alpine:latest as build\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.From from = (Docker.From) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, from.getPrefix());

        assertLiteral(from.getPlatform(), Quoting.UNQUOTED, " ", "--platform=linux/arm64", "");
        assertLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "");
        assertEquals("latest", from.getTag());
        assertLiteral(from.getVersion(), Quoting.UNQUOTED, "", ":latest", "");
        assertLiteral(from.getAs(), Quoting.UNQUOTED, " ", "as", "");
        assertLiteral(from.getAlias(), Quoting.UNQUOTED, " ", "build", "\t");
    }

    @Test
    void testFullFromWithDigest() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("FROM alpine@sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.From from = (Docker.From) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, from.getPrefix());

        assertLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "");
        assertLiteral(from.getVersion(), Quoting.UNQUOTED, "", "@sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", "\t");
        assertEquals("sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", from.getDigest());
    }

    @Test
    void testShell() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("SHELL [  \"powershell\", \"-Command\"   ]\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Shell shell = (Docker.Shell) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, shell.getPrefix());

        List<Docker.Literal> commands = shell.getCommands();
        assertEquals(2, commands.size());

        assertLiteral(commands.get(0), Quoting.DOUBLE_QUOTED, "  ", "powershell", "");
        assertLiteral(commands.get(1), Quoting.DOUBLE_QUOTED, " ", "-Command", "   ");

        assertEquals(" ", shell.getExecFormPrefix().getWhitespace());
        assertEquals("\t", shell.getExecFormSuffix().getWhitespace());
    }

    @Test
    void testShellMultiline() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("SHELL [  \"powershell\", \"-Command\" , \t\\\n\t\t  \"bash\", \"-c\"   ]".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Shell shell = (Docker.Shell) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, shell.getPrefix());

        List<Docker.Literal> commands = shell.getCommands();
        assertEquals(4, commands.size());

        assertLiteral(commands.get(0), Quoting.DOUBLE_QUOTED, "  ", "powershell", "");
        assertLiteral(commands.get(1), Quoting.DOUBLE_QUOTED, " ", "-Command", "  \t\\\n\t\t  ");
        assertLiteral(commands.get(2), Quoting.DOUBLE_QUOTED, "", "bash", "");
        assertLiteral(commands.get(3), Quoting.DOUBLE_QUOTED, " ", "-c", "   ");

        assertEquals(" ", shell.getExecFormPrefix().getWhitespace());
        assertEquals("", shell.getExecFormSuffix().getWhitespace());
    }

    @Test
    void testUserOnly() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("USER root".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.User user = (Docker.User) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, user.getPrefix());
        assertLiteral(user.getUsername(), Quoting.UNQUOTED, " ", "root", "");
        assertNull(user.getGroup());
    }

    @Test
    void testUserWithGroup() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("USER root:admin".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.User user = (Docker.User) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, user.getPrefix());
        assertLiteral(user.getUsername(), Quoting.UNQUOTED, " ", "root", "");
        assertLiteral(user.getGroup(), Quoting.UNQUOTED, "", "admin", "");
    }

    @Test
    void testUserWithFunkySpacing() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("USER    root:admin   \t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.User user = (Docker.User) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, user.getPrefix());
        assertLiteral(user.getUsername(), Quoting.UNQUOTED, "    ", "root", "");
        assertLiteral(user.getGroup(), Quoting.UNQUOTED, "", "admin", "   \t");
    }

    @Test
    void testVolumeShellFormat() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("VOLUME [ \"/var/log\", \"/var/log2\" ]\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Volume volume = (Docker.Volume) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, volume.getPrefix());
        assertEquals(" ", volume.getExecFormPrefix().getWhitespace());
        assertEquals("\t", volume.getExecFormSuffix().getWhitespace());

        List<Docker.Literal> args = volume.getPaths();
        assertEquals(2, args.size());

        assertLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "/var/log", "");
        assertLiteral(args.get(1), Quoting.DOUBLE_QUOTED, " ", "/var/log2", " ");
    }

    @Test
    void testVolumeExecFormat() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("VOLUME /var/log /var/log2\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Volume volume = (Docker.Volume) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, volume.getPrefix());
        assertEquals("", volume.getExecFormPrefix().getWhitespace());
        assertEquals("", volume.getExecFormSuffix().getWhitespace());

        List<Docker.Literal> args = volume.getPaths();
        assertEquals(2, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "/var/log", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "/var/log2", "\t");
    }

    @Test
    void testWorkDir() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("WORKDIR /var/log\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Workdir workdir = (Docker.Workdir) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, workdir.getPrefix());
        assertLiteral(workdir.getPath(), Quoting.UNQUOTED, " ", "/var/log", ""); // TODO: FIX missing traililng here.
    }

    @Test
    void testLabelSingle() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("LABEL foo=bar".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Label label = (Docker.Label) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, label.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = label.getArgs();
        assertEquals(1, args.size());

        assertRightPaddedArg(args.get(0), Quoting.UNQUOTED, " ", "foo", true, "bar", "");
    }

    @Test
    void testLabelMultiple() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("LABEL foo=bar baz=qux".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Label label = (Docker.Label) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, label.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = label.getArgs();
        assertEquals(2, args.size());

        assertRightPaddedArg(args.get(0), Quoting.UNQUOTED, " ", "foo", true, "bar", "");
        assertRightPaddedArg(args.get(1), Quoting.UNQUOTED, " ", "baz", true, "qux", "");
    }

    @Test
    void testLabelMultiline() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("LABEL foo=bar \\\n\t\tbaz=qux \\\n\t\tquux=\"Hello World\"".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Label label = (Docker.Label) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, label.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = label.getArgs();
        assertEquals(3, args.size());

        assertRightPaddedArg(args.get(0), Quoting.UNQUOTED, " ", "foo", true, "bar", " \\\n");
        assertRightPaddedArg(args.get(1), Quoting.UNQUOTED, "\t\t", "baz", true, "qux", " \\\n");
        assertRightPaddedArg(args.get(2), Quoting.DOUBLE_QUOTED, "\t\t", "quux", true, "Hello World", "");
    }


    @Test
    void testStopSignal() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("STOPSIGNAL SIGKILL".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.StopSignal stopSignal = (Docker.StopSignal) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, stopSignal.getPrefix());
        assertLiteral(stopSignal.getSignal(), Quoting.UNQUOTED, " ", "SIGKILL", "");
    }

    @Test
    void testHealthCheckNone() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("HEALTHCHECK NONE".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Healthcheck healthCheck = (Docker.Healthcheck) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, healthCheck.getPrefix());

        List<Docker.Literal> commands = healthCheck.getCommands();
        assertEquals(1, commands.size());

        assertLiteral(commands.get(0), Quoting.UNQUOTED, " ", "NONE", "");
    }

    @Test
    void testHealthCheckWithCmd() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("HEALTHCHECK --interval=5m --timeout=3s \\\n  CMD curl -f http://localhost/ || exit 1".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Healthcheck healthCheck = (Docker.Healthcheck) stage.getChildren().get(0);
        assertEquals("", healthCheck.getPrefix().getWhitespace());

        List<DockerRightPadded<Docker.KeyArgs>> options = healthCheck.getOptions();
        assertEquals(2, options.size());

        assertRightPaddedArg(options.get(0), Quoting.UNQUOTED, " ", "--interval", true, "5m", "");
        assertRightPaddedArg(options.get(1), Quoting.UNQUOTED, " ", "--timeout", true, "3s", " \\\n");

        List<Docker.Literal> commands = healthCheck.getCommands();
        assertEquals(7, commands.size());

        assertLiteral(commands.get(0), Quoting.UNQUOTED, "  ", "CMD", "");
        assertLiteral(commands.get(1), Quoting.UNQUOTED, " ", "curl", "");
        assertLiteral(commands.get(2), Quoting.UNQUOTED, " ", "-f", "");
        assertLiteral(commands.get(3), Quoting.UNQUOTED, " ", "http://localhost/", "");
        assertLiteral(commands.get(4), Quoting.UNQUOTED, " ", "||", "");
        assertLiteral(commands.get(5), Quoting.UNQUOTED, " ", "exit", "");
        assertLiteral(commands.get(6), Quoting.UNQUOTED, " ", "1", "");
    }

    @Test
    void testAdd() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ADD foo.txt \\\n\t\tbar.txt \\\n\t\tbaz.txt \\\n\t\tqux.txt /tmp/\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Add add = (Docker.Add) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, add.getPrefix());

        List<Docker.Literal> args = add.getSources();
        assertEquals(4, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "foo.txt", " \\\n\t\t");
        assertLiteral(args.get(1), Quoting.UNQUOTED, "", "bar.txt", " \\\n\t\t");
        assertLiteral(args.get(2), Quoting.UNQUOTED, "", "baz.txt", " \\\n\t\t");
        assertLiteral(args.get(3), Quoting.UNQUOTED, "", "qux.txt", "");

        Docker.Literal dest = add.getDestination();
        assertLiteral(dest, Quoting.UNQUOTED, " ", "/tmp/", "\t");
    }

    @Test
    void testAddWithOptions() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ADD --chown=foo:bar --keep-git-dir --checksum=sha256:24454f830c foo.txt \\\n\t\tbar.txt \\\n\t\tbaz.txt \\\n\t\tqux.txt /tmp/\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Add add = (Docker.Add) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, add.getPrefix());

        List<Docker.Literal> args = add.getSources();
        assertEquals(4, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "foo.txt", " \\\n\t\t");
        assertLiteral(args.get(1), Quoting.UNQUOTED, "", "bar.txt", " \\\n\t\t");
        assertLiteral(args.get(2), Quoting.UNQUOTED, "", "baz.txt", " \\\n\t\t");
        assertLiteral(args.get(3), Quoting.UNQUOTED, "", "qux.txt", "");

        Docker.Literal dest = add.getDestination();
        assertLiteral(dest, Quoting.UNQUOTED, " ", "/tmp/", "\t");

        List<Docker.Option> options = add.getOptions();
        assertEquals(3, options.size());
        assertOption(options.get(0), Quoting.UNQUOTED, " ", "--chown", true, "foo:bar");
        assertOption(options.get(1), Quoting.UNQUOTED, " ", "--keep-git-dir", false, null);
        assertOption(options.get(2), Quoting.UNQUOTED, " ", "--checksum", true, "sha256:24454f830c");
    }

    @Test
    void testCopy() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("COPY foo.txt \\\n\t\tbar.txt \\\n\t\tbaz.txt \\\n\t\tqux.txt /tmp/\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Copy copy = (Docker.Copy) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, copy.getPrefix());

        List<Docker.Literal> args = copy.getSources();
        assertEquals(4, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "foo.txt", " \\\n\t\t");
        assertLiteral(args.get(1), Quoting.UNQUOTED, "", "bar.txt", " \\\n\t\t");
        assertLiteral(args.get(2), Quoting.UNQUOTED, "", "baz.txt", " \\\n\t\t");
        assertLiteral(args.get(3), Quoting.UNQUOTED, "", "qux.txt", "");

        Docker.Literal dest = copy.getDestination();
        assertLiteral(dest, Quoting.UNQUOTED, " ", "/tmp/", "\t");
    }

    /**
     * Tests this example heredoc from docker blog @ <a href="https://www.docker.com/blog/introduction-to-heredocs-in-dockerfiles/">...</a>
     * COPY <<EOF /usr/share/nginx/html/index.html
     * (your index page goes here)
     * EOF
     */
    @Test
    void testCopyWithHeredoc() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                COPY <<EOF /usr/share/nginx/html/index.html
                (your index page goes here)
                EOF
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Copy cmd = (Docker.Copy) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getSources();
        assertEquals(4, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "<<EOF", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "/usr/share/nginx/html/index.html", "\n");
        assertLiteral(args.get(2), Quoting.UNQUOTED, "", "(your index page goes here)", "\n");
        assertLiteral(args.get(3), Quoting.UNQUOTED, "", "EOF", "\n");
    }

    @Test
    void testRun() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("RUN echo Hello World\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(3, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "\t");
    }

    @Test
    void testRunMultiline() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \\
                  --mount=type=cache,target=/var/lib/apt,sharing=locked \\
                  apt update && apt-get --no-install-recommends install -y gcc
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Option> opts = cmd.getOptions();
        assertEquals(2, opts.size());

        assertOption(opts.get(0), Quoting.UNQUOTED, " ", "--mount", true, "type=cache,target=/var/cache/apt,sharing=locked");
        assertEquals(" \\\n", opts.get(0).getTrailing().getWhitespace());

        assertOption(opts.get(1), Quoting.UNQUOTED, "  ", "--mount", true, "type=cache,target=/var/lib/apt,sharing=locked");
        assertEquals(" \\\n", opts.get(1).getTrailing().getWhitespace());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(8, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, "  ", "apt", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "update", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "&&", "");
        assertLiteral(args.get(3), Quoting.UNQUOTED, " ", "apt-get", "");
        assertLiteral(args.get(4), Quoting.UNQUOTED, " ", "--no-install-recommends", "");
        assertLiteral(args.get(5), Quoting.UNQUOTED, " ", "install", "");
        assertLiteral(args.get(6), Quoting.UNQUOTED, " ", "-y", "");
        assertLiteral(args.get(7), Quoting.UNQUOTED, " ", "gcc", "");
    }

    /**
     * Test this example heredoc from the Dockerfile reference:
     * RUN <<EOF
     * apt-get update
     * apt-get install -y curl
     * EOF
     */
    @Test
    void testRunWithHeredoc() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN <<EOF
                apt-get update
                apt-get install -y curl
                EOF
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(8, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "<<EOF", "\n");
        assertLiteral(args.get(1), Quoting.UNQUOTED, "", "apt-get", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "update", "\n");
        assertLiteral(args.get(3), Quoting.UNQUOTED, "", "apt-get", "");
        assertLiteral(args.get(4), Quoting.UNQUOTED, " ", "install", "");
        assertLiteral(args.get(5), Quoting.UNQUOTED, " ", "-y", "");
        assertLiteral(args.get(6), Quoting.UNQUOTED, " ", "curl", "\n");
        assertLiteral(args.get(7), Quoting.UNQUOTED, "", "EOF", "\n");
    }

    /**
     * Tests this example from docker blog @ <a href="https://www.docker.com/blog/introduction-to-heredocs-in-dockerfiles/">...</a>
     * RUN python3 <<EOF
     * with open("/hello", "w") as f:
     * print("Hello", file=f)
     * print("World", file=f)
     * EOF
     */
    @Test
    void testRunWithHereDocPythonExample() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN python3 <<EOF
                with open("/hello", "w") as f:
                    print("Hello", file=f)
                    print("World", file=f)
                EOF
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(9, args.size());

        // TODO: within heredocs, collect literals wrapped via () and [] along with single and double quoted strings
        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "python3", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "<<EOF", "\n");
        assertLiteral(args.get(2), Quoting.UNQUOTED, "", "with", "");
        assertLiteral(args.get(3), Quoting.UNQUOTED, " ", "open(\"/hello\", \"w\")", "");
        assertLiteral(args.get(4), Quoting.UNQUOTED, " ", "as", "");
        assertLiteral(args.get(5), Quoting.UNQUOTED, " ", "f:", "\n");
        assertLiteral(args.get(6), Quoting.UNQUOTED, "    ", "print(\"Hello\", file=f)", "\n");
        assertLiteral(args.get(7), Quoting.UNQUOTED, "    ", "print(\"World\", file=f)", "\n");
        assertLiteral(args.get(8), Quoting.UNQUOTED, "", "EOF", "\n");
    }

    /**
     * Tests this example heredoc from docker blog @ <a href="https://www.docker.com/blog/introduction-to-heredocs-in-dockerfiles/">...</a>
     * RUN python3 <<EOF > /hello
     * print("Hello")
     * print("World")
     * EOF
     */
    @Test
    void testRunWithHeredocRedirection() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN python3 <<EOF > /hello
                print("Hello")
                print("World")
                EOF
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(7, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "python3", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "<<EOF", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", ">", "");
        assertLiteral(args.get(3), Quoting.UNQUOTED, " ", "/hello", "\n");
        assertLiteral(args.get(4), Quoting.UNQUOTED, "", "print(\"Hello\")", "\n");
        assertLiteral(args.get(5), Quoting.UNQUOTED, "", "print(\"World\")", "\n");
        assertLiteral(args.get(6), Quoting.UNQUOTED, "", "EOF", "\n");
    }

    @Test
    void testRunWithHeredocAfterOtherCommands() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN echo Hello World <<EOF
                apt-get update
                apt-get install -y curl
                EOF
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(11, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "");
        assertLiteral(args.get(3), Quoting.UNQUOTED, " ", "<<EOF", "\n");
        assertLiteral(args.get(4), Quoting.UNQUOTED, "", "apt-get", "");
        assertLiteral(args.get(5), Quoting.UNQUOTED, " ", "update", "\n");
        assertLiteral(args.get(6), Quoting.UNQUOTED, "", "apt-get", "");
        assertLiteral(args.get(7), Quoting.UNQUOTED, " ", "install", "");
        assertLiteral(args.get(8), Quoting.UNQUOTED, " ", "-y", "");
        assertLiteral(args.get(9), Quoting.UNQUOTED, " ", "curl", "\n");
        assertLiteral(args.get(10), Quoting.UNQUOTED, "", "EOF", "\n");
    }

    @Test
    void testFullDockerfile() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                FROM alpine:latest
                RUN echo Hello World
                CMD echo Goodbye World
                ENTRYPOINT [ "echo", "Hello" ]
                EXPOSE 8080 8081 \\\n\t\t9999/udp
                SHELL [ "powershell", "-Command" ]
                USER root:admin
                VOLUME [ "/var/log", "/var/log2" ]
                WORKDIR /var/log
                LABEL foo=bar baz=qux
                STOPSIGNAL SIGKILL
                HEALTHCHECK NONE
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 12);

        Docker.From from = (Docker.From) stage.getChildren().get(0);

        assertLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "");
        assertLiteral(from.getVersion(), Quoting.UNQUOTED, "", ":latest", "");
        assertEquals("latest", from.getTag());
        assertLiteral(from.getPlatform(), Quoting.UNQUOTED, "", null, "");
        assertLiteral(from.getAlias(), Quoting.UNQUOTED, "", null, "");
        assertLiteral(from.getAs(), Quoting.UNQUOTED, "", null, "");

        Docker.Run run = (Docker.Run) stage.getChildren().get(1);
        assertEquals(Space.EMPTY, run.getPrefix());
        assertLiteral(run.getCommands().get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(run.getCommands().get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(run.getCommands().get(2), Quoting.UNQUOTED, " ", "World", "");


        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(2);
        assertEquals(Space.EMPTY, cmd.getPrefix());
        assertLiteral(cmd.getCommands().get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(cmd.getCommands().get(1), Quoting.UNQUOTED, " ", "Goodbye", "");
        assertLiteral(cmd.getCommands().get(2), Quoting.UNQUOTED, " ", "World", "");

        Docker.Entrypoint entryPoint = (Docker.Entrypoint) stage.getChildren().get(3);
        assertEquals(Space.EMPTY, entryPoint.getPrefix());
        assertEquals(2, entryPoint.getCommands().size());
        assertEquals(Form.EXEC, entryPoint.getForm());
        assertLiteral(entryPoint.getCommands().get(0), Quoting.DOUBLE_QUOTED, " ", "echo", "");
        assertLiteral(entryPoint.getCommands().get(1), Quoting.DOUBLE_QUOTED, " ", "Hello", " ");
        assertEquals(" ", entryPoint.getExecFormPrefix().getWhitespace());
        assertEquals("", entryPoint.getExecFormSuffix().getWhitespace());

        Docker.Expose expose = (Docker.Expose) stage.getChildren().get(4);
        assertEquals(Space.EMPTY, expose.getPrefix());
        assertEquals(" ", expose.getPorts().get(0).getElement().getPrefix().getWhitespace());
        assertEquals("8080", expose.getPorts().get(0).getElement().getPort());
        assertEquals("tcp", expose.getPorts().get(0).getElement().getProtocol());
        assertFalse(expose.getPorts().get(0).getElement().isProtocolProvided());

        assertEquals(" ", expose.getPorts().get(1).getElement().getPrefix().getWhitespace());
        assertEquals("8081", expose.getPorts().get(1).getElement().getPort());
        assertEquals("tcp", expose.getPorts().get(1).getElement().getProtocol());
        assertFalse(expose.getPorts().get(1).getElement().isProtocolProvided());
        assertEquals(" \\\n", expose.getPorts().get(1).getAfter().getWhitespace());

        assertEquals("", expose.getPorts().get(2).getAfter().getWhitespace());
        assertEquals("\t\t", expose.getPorts().get(2).getElement().getPrefix().getWhitespace());
        assertEquals("9999", expose.getPorts().get(2).getElement().getPort());
        assertEquals("udp", expose.getPorts().get(2).getElement().getProtocol());
        assertTrue(expose.getPorts().get(2).getElement().isProtocolProvided());

        Docker.Shell shell = (Docker.Shell) stage.getChildren().get(5);
        assertEquals(Space.EMPTY, shell.getPrefix());
        assertEquals(2, shell.getCommands().size());
        assertLiteral(shell.getCommands().get(0), Quoting.DOUBLE_QUOTED, " ", "powershell", "");
        assertLiteral(shell.getCommands().get(1), Quoting.DOUBLE_QUOTED, " ", "-Command", " ");
        assertEquals(" ", shell.getExecFormPrefix().getWhitespace());
        assertEquals("", shell.getExecFormSuffix().getWhitespace());

        Docker.User user = (Docker.User) stage.getChildren().get(6);
        assertEquals(Space.EMPTY, user.getPrefix());
        assertLiteral(user.getUsername(), Quoting.UNQUOTED, " ", "root", "");
        assertLiteral(user.getGroup(), Quoting.UNQUOTED, "", "admin", "");

        Docker.Volume volume = (Docker.Volume) stage.getChildren().get(7);
        assertEquals(Space.EMPTY, volume.getPrefix());
        assertEquals(" ", volume.getExecFormPrefix().getWhitespace());
        assertEquals("", volume.getExecFormSuffix().getWhitespace());
        assertEquals(2, volume.getPaths().size());
        assertLiteral(volume.getPaths().get(0), Quoting.DOUBLE_QUOTED, " ", "/var/log", "");
        assertLiteral(volume.getPaths().get(1), Quoting.DOUBLE_QUOTED, " ", "/var/log2", " ");

        Docker.Workdir workdir = (Docker.Workdir) stage.getChildren().get(8);
        assertEquals(Space.EMPTY, workdir.getPrefix());
        assertLiteral(workdir.getPath(), Quoting.UNQUOTED, " ", "/var/log", "");

        Docker.Label label = (Docker.Label) stage.getChildren().get(9);
        assertEquals(Space.EMPTY, label.getPrefix());
        assertEquals(2, label.getArgs().size());
        assertRightPaddedArg(label.getArgs().get(0), Quoting.UNQUOTED, " ", "foo", true, "bar", "");
        assertRightPaddedArg(label.getArgs().get(1), Quoting.UNQUOTED, " ", "baz", true, "qux", "");

        Docker.StopSignal stopSignal = (Docker.StopSignal) stage.getChildren().get(10);
        assertEquals(Space.EMPTY, stopSignal.getPrefix());
        assertLiteral(stopSignal.getSignal(), Quoting.UNQUOTED, " ", "SIGKILL", "");

        Docker.Healthcheck healthCheck = (Docker.Healthcheck) stage.getChildren().get(11);
        assertEquals(Space.EMPTY, healthCheck.getPrefix());
        assertEquals(1, healthCheck.getCommands().size());
        assertLiteral(healthCheck.getCommands().get(0), Quoting.UNQUOTED, " ", "NONE", "");
    }

    @Test
    void handleMultipleNewlinesEOF() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN echo Hello World
                
                
                
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);
        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());
        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(3, args.size());
        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "");
        assertEquals(printableWhiteSpace("\n\n\n"), printableWhiteSpace(doc.getEof().getWhitespace()));
    }

    @Test
    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    void handleMultipleCRLFinEOF() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN echo Hello World
                \r
                \r
                \r 
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);
        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());
        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(3, args.size());
        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "");
        assertEquals(printableWhiteSpace("\r\n\r\n\r\n"), printableWhiteSpace(doc.getEof().getWhitespace()));
    }

    @Test
    void handleMultipleStagesWithoutAliasNames() {
        DockerfileParser parser = new DockerfileParser();

        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                FROM ubuntu:20.04
                RUN apt-get update && apt-get install -y build-essential
                RUN echo "Stage 1 complete" > /stage1.txt
                
                FROM ubuntu:20.04
                COPY --from=0 /stage1.txt /stage2.txt
                RUN echo "Stage 2 complete" > /stage2_complete.txt
                
                FROM ubuntu:20.04
                COPY --from=1 /stage2_complete.txt /stage3.txt
                RUN echo "Stage 3 complete" > /stage3_complete.txt
                
                FROM ubuntu:20.04
                COPY --from=2 /stage3_complete.txt /final_stage.txt
                CMD ["cat", "/final_stage.txt"]
                """.getBytes(StandardCharsets.UTF_8)));


        assertNotNull(doc, "Expected document to be non-null but was null");
        assertNotNull(doc.getStages(),
                "Expected document to have stages but was null");
        assertEquals(4, doc.getStages().size(),
                "Expected document to have " + 4 + " stage but was " + doc.getStages().size());

        Docker.Stage stage = doc.getStages().get(0);
        assertNotNull(stage.getChildren(),
                "Expected stage to have children but was null");

        for (Docker.Stage docStage : doc.getStages()) {
            assertNotNull(docStage.getChildren(),
                    "Expected stage to have children but was null");
            assertEquals(3, docStage.getChildren().size(),
                    "Expected every stage to have only 3 children but was " + stage.getChildren().size());
        }

        Docker.Stage stage0 = doc.getStages().get(0);
        Docker.Stage stage1 = doc.getStages().get(1);
        Docker.Stage stage2 = doc.getStages().get(2);
        Docker.Stage finalStage = doc.getStages().get(3);

        Docker.From from = (Docker.From) stage0.getChildren().get(0);
        assertEquals(Space.EMPTY, from.getPrefix());
        assertLiteral(from.getImage(), Quoting.UNQUOTED, " ", "ubuntu", "");
        assertLiteral(from.getVersion(), Quoting.UNQUOTED, "", ":20.04", "");
        assertEquals("20.04", from.getTag());
        assertLiteral(from.getPlatform(), Quoting.UNQUOTED, "", null, "");
        assertLiteral(from.getAlias(), Quoting.UNQUOTED, "", null, "");


        Docker.Run run = (Docker.Run) stage0.getChildren().get(1);
        assertEquals(Space.EMPTY, run.getPrefix());
        List<Docker.Literal> commands = run.getCommands();
        assertEquals(7, commands.size());
        assertLiteral(commands.get(0), Quoting.UNQUOTED, " ", "apt-get", "");
        assertLiteral(commands.get(1), Quoting.UNQUOTED, " ", "update", "");
        assertLiteral(commands.get(2), Quoting.UNQUOTED, " ", "&&", "");
        assertLiteral(commands.get(3), Quoting.UNQUOTED, " ", "apt-get", "");
        assertLiteral(commands.get(4), Quoting.UNQUOTED, " ", "install", "");
        assertLiteral(commands.get(5), Quoting.UNQUOTED, " ", "-y", "");
        assertLiteral(commands.get(6), Quoting.UNQUOTED, " ", "build-essential", "");


        run = (Docker.Run) stage0.getChildren().get(2);
        assertEquals(Space.EMPTY, run.getPrefix());
        commands = run.getCommands();
        assertEquals(4, commands.size());
        assertLiteral(commands.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(commands.get(1), Quoting.DOUBLE_QUOTED, " ", "Stage 1 complete", "");
        assertLiteral(commands.get(2), Quoting.UNQUOTED, " ", ">", "");
        assertLiteral(commands.get(3), Quoting.UNQUOTED, " ", "/stage1.txt", "");


        Docker.From from1 = (Docker.From) stage1.getChildren().get(0);
        assertEquals(Space.NEWLINE, from1.getPrefix());
        assertLiteral(from1.getImage(), Quoting.UNQUOTED, " ", "ubuntu", "");
        assertLiteral(from1.getVersion(), Quoting.UNQUOTED, "", ":20.04", "");
        assertEquals("20.04", from1.getTag());
        assertLiteral(from1.getPlatform(), Quoting.UNQUOTED, "", null, "");
        assertLiteral(from1.getAlias(), Quoting.UNQUOTED, "", null, "");

        Docker.Copy copy = (Docker.Copy) stage1.getChildren().get(1);
        assertEquals(Space.EMPTY, copy.getPrefix());
        List<Docker.Option> opts = copy.getOptions();
        assertOption(opts.get(0), Quoting.UNQUOTED, " ", "--from", true, "0");

        List<Docker.Literal> sources = copy.getSources();
        assertEquals(1, sources.size());
        assertLiteral(sources.get(0), Quoting.UNQUOTED, " ", "/stage1.txt", "");
        Docker.Literal dest = copy.getDestination();
        assertLiteral(dest, Quoting.UNQUOTED, " ", "/stage2.txt", "");

        run = (Docker.Run) stage1.getChildren().get(2);
        assertEquals(Space.EMPTY, run.getPrefix());
        commands = run.getCommands();
        assertEquals(4, commands.size());
        assertLiteral(commands.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(commands.get(1), Quoting.DOUBLE_QUOTED, " ", "Stage 2 complete", "");
        assertLiteral(commands.get(2), Quoting.UNQUOTED, " ", ">", "");
        assertLiteral(commands.get(3), Quoting.UNQUOTED, " ", "/stage2_complete.txt", "");

        Docker.From from2 = (Docker.From) stage2.getChildren().get(0);
        assertEquals(Space.NEWLINE, from2.getPrefix());
        assertLiteral(from2.getImage(), Quoting.UNQUOTED, " ", "ubuntu", "");
        assertLiteral(from2.getVersion(), Quoting.UNQUOTED, "", ":20.04", "");
        assertEquals("20.04", from2.getTag());
        assertLiteral(from2.getPlatform(), Quoting.UNQUOTED, "", null, "");
        assertLiteral(from2.getAlias(), Quoting.UNQUOTED, "", null, "");

        copy = (Docker.Copy) stage2.getChildren().get(1);
        assertEquals(Space.EMPTY, copy.getPrefix());
        opts = copy.getOptions();
        sources = copy.getSources();
        assertEquals(1, sources.size());

        assertOption(opts.get(0), Quoting.UNQUOTED, " ", "--from", true, "1");
        assertLiteral(sources.get(0), Quoting.UNQUOTED, " ", "/stage2_complete.txt", "");
        dest = copy.getDestination();
        assertLiteral(dest, Quoting.UNQUOTED, " ", "/stage3.txt", "");
        run = (Docker.Run) stage2.getChildren().get(2);
        assertEquals(Space.EMPTY, run.getPrefix());
        commands = run.getCommands();
        assertEquals(4, commands.size());
        assertLiteral(commands.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(commands.get(1), Quoting.DOUBLE_QUOTED, " ", "Stage 3 complete", "");
        assertLiteral(commands.get(2), Quoting.UNQUOTED, " ", ">", "");
        assertLiteral(commands.get(3), Quoting.UNQUOTED, " ", "/stage3_complete.txt", "");

        Docker.From from3 = (Docker.From) finalStage.getChildren().get(0);
        assertEquals(Space.NEWLINE, from3.getPrefix());
        assertLiteral(from3.getImage(), Quoting.UNQUOTED, " ", "ubuntu", "");
        assertLiteral(from3.getVersion(), Quoting.UNQUOTED, "", ":20.04", "");
        assertEquals("20.04", from3.getTag());
        assertLiteral(from3.getPlatform(), Quoting.UNQUOTED, "", null, "");
        assertLiteral(from3.getAlias(), Quoting.UNQUOTED, "", null, "");

        copy = (Docker.Copy) finalStage.getChildren().get(1);
        assertEquals(Space.EMPTY, copy.getPrefix());

        opts = copy.getOptions();
        sources = copy.getSources();
        assertEquals(1, sources.size());
        assertOption(opts.get(0), Quoting.UNQUOTED, " ", "--from", true, "2");
        assertLiteral(sources.get(0), Quoting.UNQUOTED, " ", "/stage3_complete.txt", "");
        dest = copy.getDestination();
        assertLiteral(dest, Quoting.UNQUOTED, " ", "/final_stage.txt", "");

        Docker.Cmd cmd = (Docker.Cmd) finalStage.getChildren().get(2);
        assertEquals(Space.EMPTY, cmd.getPrefix());
        assertLiteral(cmd.getCommands().get(0), Quoting.DOUBLE_QUOTED, "", "cat", "");
        assertLiteral(cmd.getCommands().get(1), Quoting.DOUBLE_QUOTED, " ", "/final_stage.txt", "");
        assertEquals(" ", cmd.getExecFormPrefix().getWhitespace());
    }

    @Test
    void testDockerfileWithCommentBetweenContinuationLines() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream(
                """
                RUN echo Hello World \\
                # This is a comment
                && echo Goodbye World
                """.getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<Docker.Literal> args = cmd.getCommands();
        assertEquals(11, args.size());

        assertLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "");
        assertLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", " \\\n");
        assertLiteral(args.get(3), Quoting.UNQUOTED, "", "#", "");
        assertLiteral(args.get(4), Quoting.UNQUOTED, " ", "This", "");
        assertLiteral(args.get(5), Quoting.UNQUOTED, " ", "is", "");
        assertLiteral(args.get(6), Quoting.UNQUOTED, " ", "a", "");

        // TODO: this should be two separate literals
        assertLiteral(args.get(7), Quoting.UNQUOTED, " ", "comment\n&&", "");
        assertLiteral(args.get(8), Quoting.UNQUOTED, " ", "echo", "");
        assertLiteral(args.get(9), Quoting.UNQUOTED, " ", "Goodbye", "");
        assertLiteral(args.get(10), Quoting.UNQUOTED, " ", "World", "");

    }
}

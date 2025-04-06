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

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(2, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.DOUBLE_QUOTED, " ", "Hello World", " ", "");

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

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(3, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "", "");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "", "   ");
    }

    @Test
    void testCmdShellFormWithQuotes() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("CMD \"echo Hello World\"   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(1, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo Hello World", "", "   ");
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

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(3, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "", "");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "", "   ");
    }

    @Test
    void testEntrypointComplexExecForm() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT [ \"echo\", \"Hello World\" ]   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Entrypoint entrypoint = (Docker.Entrypoint) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, entrypoint.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = entrypoint.getCommands();
        assertEquals(2, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.DOUBLE_QUOTED, " ", "Hello World", " ", "");

        assertEquals("   ", entrypoint.getExecFormSuffix().getWhitespace());
    }

    @Test
    void testEntrypointShellForm() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT echo Hello World   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Entrypoint entrypoint = (Docker.Entrypoint) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, entrypoint.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = entrypoint.getCommands();
        assertEquals(3, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "", "");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "", "   ");
    }

    @Test
    void testEntrypointShellFormWithQuotes() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT \"echo Hello World\"   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Entrypoint entrypoint = (Docker.Entrypoint) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, entrypoint.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = entrypoint.getCommands();
        assertEquals(1, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "echo Hello World", "", "   ");
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

        assertRightPaddedLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "", "");
        assertEquals("latest", from.getTag());
        assertEquals("", from.getImage().getAfter().getWhitespace());
    }

    @Test
    void testFullFrom() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("FROM --platform=linux/arm64 alpine:latest as build\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.From from = (Docker.From) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, from.getPrefix());

        assertRightPaddedLiteral(from.getPlatform(), Quoting.UNQUOTED, " ", "--platform=linux/arm64", "", "");
        assertRightPaddedLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "", "");
        assertEquals("latest", from.getTag());
        assertRightPaddedLiteral(from.getVersion(), Quoting.UNQUOTED, "", ":latest", "", "");
        assertLiteral(from.getAs(), Quoting.UNQUOTED, " ", "as", "");
        assertRightPaddedLiteral(from.getAlias(), Quoting.UNQUOTED, " ", "build", "", "\t");
    }

    @Test
    void testFullFromWithDigest() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("FROM alpine@sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.From from = (Docker.From) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, from.getPrefix());

        assertRightPaddedLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "", "");
        assertRightPaddedLiteral(from.getVersion(), Quoting.UNQUOTED, "", "@sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", "", "\t");
        assertEquals("sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", from.getDigest());
    }

    @Test
    void testShell() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("SHELL [  \"powershell\", \"-Command\"   ]\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Shell shell = (Docker.Shell) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, shell.getPrefix());

        List<DockerRightPadded<Docker.Literal>> commands = shell.getCommands();
        assertEquals(2, commands.size());

        assertRightPaddedLiteral(commands.get(0), Quoting.DOUBLE_QUOTED, "  ", "powershell", "", "");
        assertRightPaddedLiteral(commands.get(1), Quoting.DOUBLE_QUOTED, " ", "-Command", "   ", "");

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

        List<DockerRightPadded<Docker.Literal>> commands = shell.getCommands();
        assertEquals(4, commands.size());

        assertRightPaddedLiteral(commands.get(0), Quoting.DOUBLE_QUOTED, "  ", "powershell", "", "");
        assertRightPaddedLiteral(commands.get(1), Quoting.DOUBLE_QUOTED, " ", "-Command", " ", " \t\\\n\t\t  ");
        assertRightPaddedLiteral(commands.get(2), Quoting.DOUBLE_QUOTED, "", "bash", "", "");
        assertRightPaddedLiteral(commands.get(3), Quoting.DOUBLE_QUOTED, " ", "-c", "   ", "");

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

        List<DockerRightPadded<Docker.Literal>> args = volume.getPaths();
        assertEquals(2, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.DOUBLE_QUOTED, " ", "/var/log", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.DOUBLE_QUOTED, " ", "/var/log2", " ", "");
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

        List<DockerRightPadded<Docker.Literal>> args = volume.getPaths();
        assertEquals(2, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "/var/log", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, " ", "/var/log2", "", "\t");
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

        List<DockerRightPadded<Docker.Literal>> commands = healthCheck.getCommands();
        assertEquals(1, commands.size());

        assertRightPaddedLiteral(commands.get(0), Quoting.UNQUOTED, " ", "NONE", "", "");
    }

    @Test
    void testHealthCheckWithCmd() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("HEALTHCHECK --interval=5m --timeout=3s \\\n  CMD curl -f http://localhost/ || exit 1".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Healthcheck healthCheck = (Docker.Healthcheck) stage.getChildren().get(0);
        assertEquals(" ", healthCheck.getPrefix().getWhitespace());

        List<DockerRightPadded<Docker.KeyArgs>> options = healthCheck.getOptions();
        assertEquals(2, options.size());

        assertRightPaddedArg(options.get(0), Quoting.UNQUOTED, " ", "--interval", true, "5m", "");
        assertRightPaddedArg(options.get(1), Quoting.UNQUOTED, " ", "--timeout", true, "3s", " \\\n");

        List<DockerRightPadded<Docker.Literal>> commands = healthCheck.getCommands();
        assertEquals(7, commands.size());

        assertRightPaddedLiteral(commands.get(0), Quoting.UNQUOTED, "  ", "CMD", "", "");
        assertRightPaddedLiteral(commands.get(1), Quoting.UNQUOTED, " ", "curl", "", "");
        assertRightPaddedLiteral(commands.get(2), Quoting.UNQUOTED, " ", "-f", "", "");
        assertRightPaddedLiteral(commands.get(3), Quoting.UNQUOTED, " ", "http://localhost/", "", "");
        assertRightPaddedLiteral(commands.get(4), Quoting.UNQUOTED, " ", "||", "", "");
        assertRightPaddedLiteral(commands.get(5), Quoting.UNQUOTED, " ", "exit", "", "");
        assertRightPaddedLiteral(commands.get(6), Quoting.UNQUOTED, " ", "1", "", "");
    }

    @Test
    void testAdd() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ADD foo.txt \\\n\t\tbar.txt \\\n\t\tbaz.txt \\\n\t\tqux.txt /tmp/\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Add add = (Docker.Add) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, add.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = add.getSources();
        assertEquals(4, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "foo.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, "", "bar.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, "", "baz.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(3), Quoting.UNQUOTED, "", "qux.txt", "", "");

        DockerRightPadded<Docker.Literal> dest = add.getDestination();
        assertRightPaddedLiteral(dest, Quoting.UNQUOTED, " ", "/tmp/", "", "\t");
    }

    @Test
    void testAddWithOptions() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ADD --chown=foo:bar --keep-git-dir --checksum=sha256:24454f830c foo.txt \\\n\t\tbar.txt \\\n\t\tbaz.txt \\\n\t\tqux.txt /tmp/\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Add add = (Docker.Add) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, add.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = add.getSources();
        assertEquals(4, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "foo.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, "", "bar.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, "", "baz.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(3), Quoting.UNQUOTED, "", "qux.txt", "", "");

        DockerRightPadded<Docker.Literal> dest = add.getDestination();
        assertRightPaddedLiteral(dest, Quoting.UNQUOTED, " ", "/tmp/", "", "\t");

        List<DockerRightPadded<Docker.Option>> options = add.getOptions();
        assertEquals(3, options.size());

        assertOption(options.get(0).getElement(), Quoting.UNQUOTED, " ", "--chown", true, "foo:bar");
        assertEquals("", options.get(0).getAfter().getWhitespace());

        assertOption(options.get(1).getElement(), Quoting.UNQUOTED, " ", "--keep-git-dir", false, null);
        assertEquals("", options.get(1).getAfter().getWhitespace());

        assertOption(options.get(2).getElement(), Quoting.UNQUOTED, " ", "--checksum", true, "sha256:24454f830c");
        assertEquals("", options.get(2).getAfter().getWhitespace());
    }

    @Test
    void testCopy() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("COPY foo.txt \\\n\t\tbar.txt \\\n\t\tbaz.txt \\\n\t\tqux.txt /tmp/\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Copy copy = (Docker.Copy) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, copy.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = copy.getSources();
        assertEquals(4, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "foo.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, "", "bar.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, "", "baz.txt", "", " \\\n\t\t");
        assertRightPaddedLiteral(args.get(3), Quoting.UNQUOTED, "", "qux.txt", "", "");

        DockerRightPadded<Docker.Literal> dest = copy.getDestination();
        assertRightPaddedLiteral(dest, Quoting.UNQUOTED, " ", "/tmp/", "", "\t");
    }

    @Test
    void testRun() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("RUN echo Hello World\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Run cmd = (Docker.Run) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(3, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "", "");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "", "\t");
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

        List<DockerRightPadded<Docker.Option>> opts = cmd.getOptions();
        assertEquals(2, opts.size());

        assertOption(opts.get(0).getElement(), Quoting.UNQUOTED, " ", "--mount", true, "type=cache,target=/var/cache/apt,sharing=locked");
        assertEquals(" \\\n", opts.get(0).getAfter().getWhitespace());

        assertOption(opts.get(1).getElement(), Quoting.UNQUOTED, "  ", "--mount", true, "type=cache,target=/var/lib/apt,sharing=locked");
        assertEquals(" \\\n", opts.get(1).getAfter().getWhitespace());

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(8, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, "  ", "apt", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, " ", "update", "", "");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, " ", "&&", "", "");
        assertRightPaddedLiteral(args.get(3), Quoting.UNQUOTED, " ", "apt-get", "", "");
        assertRightPaddedLiteral(args.get(4), Quoting.UNQUOTED, " ", "--no-install-recommends", "", "");
        assertRightPaddedLiteral(args.get(5), Quoting.UNQUOTED, " ", "install", "", "");
        assertRightPaddedLiteral(args.get(6), Quoting.UNQUOTED, " ", "-y", "", "");
        assertRightPaddedLiteral(args.get(7), Quoting.UNQUOTED, " ", "gcc", "", "");
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

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(8, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "<<EOF", "", "\n");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, "", "apt-get", "", "");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, " ", "update", "", "\n");
        assertRightPaddedLiteral(args.get(3), Quoting.UNQUOTED, "", "apt-get", "", "");
        assertRightPaddedLiteral(args.get(4), Quoting.UNQUOTED, " ", "install", "", "");
        assertRightPaddedLiteral(args.get(5), Quoting.UNQUOTED, " ", "-y", "", "");
        assertRightPaddedLiteral(args.get(6), Quoting.UNQUOTED, " ", "curl", "", "\n");
        assertRightPaddedLiteral(args.get(7), Quoting.UNQUOTED, "", "EOF", "", "");
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

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(11, args.size());

        assertRightPaddedLiteral(args.get(0), Quoting.UNQUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(args.get(1), Quoting.UNQUOTED, " ", "Hello", "", "");
        assertRightPaddedLiteral(args.get(2), Quoting.UNQUOTED, " ", "World", "", "");
        assertRightPaddedLiteral(args.get(3), Quoting.UNQUOTED, " ", "<<EOF", "", "\n");
        assertRightPaddedLiteral(args.get(4), Quoting.UNQUOTED, "", "apt-get", "", "");
        assertRightPaddedLiteral(args.get(5), Quoting.UNQUOTED, " ", "update", "", "\n");
        assertRightPaddedLiteral(args.get(6), Quoting.UNQUOTED, "", "apt-get", "", "");
        assertRightPaddedLiteral(args.get(7), Quoting.UNQUOTED, " ", "install", "", "");
        assertRightPaddedLiteral(args.get(8), Quoting.UNQUOTED, " ", "-y", "", "");
        assertRightPaddedLiteral(args.get(9), Quoting.UNQUOTED, " ", "curl", "", "\n");
        assertRightPaddedLiteral(args.get(10), Quoting.UNQUOTED, "", "EOF", "", "");
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

        assertRightPaddedLiteral(from.getImage(), Quoting.UNQUOTED, " ", "alpine", "", "");
        assertRightPaddedLiteral(from.getVersion(), Quoting.UNQUOTED, "", ":latest", "", "");
        assertEquals("latest", from.getTag());
        assertRightPaddedLiteral(from.getPlatform(), Quoting.UNQUOTED, "", null, "", "");
        assertRightPaddedLiteral(from.getAlias(), Quoting.UNQUOTED, "", null, "", "");
        assertLiteral(from.getAs(), Quoting.UNQUOTED, "", null, "");

        Docker.Run run = (Docker.Run) stage.getChildren().get(1);
        assertEquals(Space.EMPTY, run.getPrefix());
        assertRightPaddedLiteral(run.getCommands().get(0), Quoting.UNQUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(run.getCommands().get(1), Quoting.UNQUOTED, " ", "Hello", "", "");
        assertRightPaddedLiteral(run.getCommands().get(2), Quoting.UNQUOTED, " ", "World", "", "");


        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(2);
        assertEquals(Space.EMPTY, cmd.getPrefix());
        assertRightPaddedLiteral(cmd.getCommands().get(0), Quoting.UNQUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(cmd.getCommands().get(1), Quoting.UNQUOTED, " ", "Goodbye", "", "");
        assertRightPaddedLiteral(cmd.getCommands().get(2), Quoting.UNQUOTED, " ", "World", "", "");

        Docker.Entrypoint entryPoint = (Docker.Entrypoint) stage.getChildren().get(3);
        assertEquals(Space.EMPTY, entryPoint.getPrefix());
        assertEquals(2, entryPoint.getCommands().size());
        assertEquals(Form.EXEC, entryPoint.getForm());
        assertRightPaddedLiteral(entryPoint.getCommands().get(0), Quoting.DOUBLE_QUOTED, " ", "echo", "", "");
        assertRightPaddedLiteral(entryPoint.getCommands().get(1), Quoting.DOUBLE_QUOTED, " ", "Hello", " ", "");
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
        assertRightPaddedLiteral(shell.getCommands().get(0), Quoting.DOUBLE_QUOTED, " ", "powershell", "", "");
        assertRightPaddedLiteral(shell.getCommands().get(1), Quoting.DOUBLE_QUOTED, " ", "-Command", " ", "");
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
        assertRightPaddedLiteral(volume.getPaths().get(0), Quoting.DOUBLE_QUOTED, " ", "/var/log", "", "");
        assertRightPaddedLiteral(volume.getPaths().get(1), Quoting.DOUBLE_QUOTED, " ", "/var/log2", " ", "");

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
        assertRightPaddedLiteral(healthCheck.getCommands().get(0), Quoting.UNQUOTED, " ", "NONE", "", "");
    }
}
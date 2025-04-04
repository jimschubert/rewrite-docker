package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Quoting;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.jimschubert.rewrite.docker.internal.DockerParserHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class DockerfileParserTest {
    @Test
    void testCommentSingleWithTrailingSpace() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("# This is a comment\t\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Comment comment = (Docker.Comment) stage.getChildren().get(0);
        assertComment(comment, "This is a comment", " ", "\t\t");
    }

    @Test
    void testDirective() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("#    syntax=docker/dockerfile:1".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Directive directive = (Docker.Directive) stage.getChildren().get(0);
        assertDirective(directive, "syntax", true, "docker/dockerfile:1", "    ", "");
    }

    @Test
    void testArgNoAssignment() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ARG foo".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Arg arg = (Docker.Arg) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, arg.getPrefix());
        List<DockerRightPadded<Docker.KeyArgs>> args = arg.getArgs();

        assertArg(args.get(0), "foo", false, null, " ", "", Quoting.UNQUOTED);
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

        assertArg(args.get(0), "foo", true, "bar", " ", "", Quoting.UNQUOTED);
        assertArg(args.get(1), "baz", false, null, " ", "", Quoting.UNQUOTED);
        assertArg(args.get(2), "MY_VAR", false, null, " ", "", Quoting.UNQUOTED);
        assertArg(args.get(3), "OTHER_VAR", true, "some default", " ", " \t", Quoting.DOUBLE_QUOTED);
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

        assertArg(args.get(0), "foo", true, "bar", " ", "", Quoting.UNQUOTED);
        assertArg(args.get(1), "baz", false, null, " ", "", Quoting.UNQUOTED);
        assertArg(args.get(2), "MY_VAR", false, null, " ", " \\\n", Quoting.UNQUOTED);
        assertArg(args.get(3), "OTHER_VAR", true, "some default", "", " \t\\\n", Quoting.DOUBLE_QUOTED);
        assertArg(args.get(4), "LAST", false, null, "\t\t", "", Quoting.UNQUOTED);
    }

    @Test
    void testCmdComplexExecForm(){
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("CMD [ \"echo\", \"Hello World\" ]   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Cmd cmd = (Docker.Cmd) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, cmd.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(2, args.size());

        assertEquals("echo", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("Hello World", args.get(1).getElement().getText());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(1).getElement().getTrailing().getWhitespace());
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
        assertEquals("echo", args.get(0).getElement().getText());
        assertEquals("Hello", args.get(1).getElement().getText());
        assertEquals("World", args.get(2).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("   ", args.get(2).getAfter().getWhitespace());
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
        assertEquals("echo Hello World", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("   ", args.get(0).getAfter().getWhitespace());
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
        assertEquals("echo", args.get(0).getElement().getText());
        assertEquals("Hello", args.get(1).getElement().getText());
        assertEquals("World", args.get(2).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("   ", args.get(2).getAfter().getWhitespace());
    }

    @Test
    void testEntrypointComplexExecForm(){
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT [ \"echo\", \"Hello World\" ]   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Entrypoint entrypoint = (Docker.Entrypoint) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, entrypoint.getPrefix());

        List<DockerRightPadded<Docker.Literal>> args = entrypoint.getCommands();
        assertEquals(2, args.size());

        assertEquals("echo", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("Hello World", args.get(1).getElement().getText());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(1).getElement().getTrailing().getWhitespace());
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
        assertEquals("echo", args.get(0).getElement().getText());
        assertEquals("Hello", args.get(1).getElement().getText());
        assertEquals("World", args.get(2).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("   ", args.get(2).getAfter().getWhitespace());
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
        assertEquals("echo Hello World", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("   ", args.get(0).getAfter().getWhitespace());
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

        assertArg(args.get(0), "MY_NAME", true, "John Doe", " ", "", Quoting.DOUBLE_QUOTED);
        assertArg(args.get(2), "MY_CAT", true, "fluffy", " ", "", Quoting.UNQUOTED);
        assertArg(args.get(1), "MY_DOG", true, "Rex The Dog", " ", "", Quoting.UNQUOTED);
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

        assertArg(args.get(0), "MY_NAME", true, "John Doe", " ", "", Quoting.DOUBLE_QUOTED);
        assertArg(args.get(1), "MY_DOG", true, "Rex The Dog", " ", " \\\n", Quoting.UNQUOTED);
        assertArg(args.get(2), "MY_CAT", true, "fluffy", "", " ", Quoting.UNQUOTED);
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
        assertEquals("alpine", from.getImage().getElement().getText());
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
        assertEquals("--platform=linux/arm64", from.getPlatform().getElement().getText());
        assertEquals("alpine", from.getImage().getElement().getText());
        assertEquals("latest", from.getTag());
        assertEquals("", from.getImage().getAfter().getWhitespace());

        assertEquals("as", from.getAs().getText());
        assertEquals(" ", from.getAs().getPrefix().getWhitespace());

        assertEquals(" ", from.getAlias().getElement().getPrefix().getWhitespace());
        assertEquals("build", from.getAlias().getElement().getText());
        assertEquals("\t", from.getAlias().getAfter().getWhitespace());
    }

    @Test
    void testFullFromWithDigest() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("FROM alpine@sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.From from = (Docker.From) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, from.getPrefix());
        assertEquals("alpine", from.getImage().getElement().getText());
        assertEquals("sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", from.getDigest());
        assertEquals("", from.getImage().getAfter().getWhitespace());
    }

    @Test
    void testShell() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("SHELL [  \"powershell\", \"-Command\"   ]\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Shell shell = (Docker.Shell) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, shell.getPrefix());
        assertEquals("powershell", shell.getCommands().get(0).getElement().getText());
        assertEquals("  ", shell.getCommands().get(0).getElement().getPrefix().getWhitespace());
        assertEquals("-Command", shell.getCommands().get(1).getElement().getText());
        assertEquals(" ", shell.getCommands().get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" ", shell.getExecFormPrefix().getWhitespace());
        assertEquals("   ", shell.getCommands().get(1).getElement().getTrailing().getWhitespace());
        assertEquals("\t", shell.getExecFormSuffix().getWhitespace());
    }

    @Test
    void testShellMultiline(){
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("SHELL [  \"powershell\", \"-Command\" , \t\\\n\t\t  \"bash\", \"-c\"   ]".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Shell shell = (Docker.Shell) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, shell.getPrefix());

        assertEquals("  ", shell.getCommands().get(0).getElement().getPrefix().getWhitespace());
        assertEquals("powershell", shell.getCommands().get(0).getElement().getText());
        assertEquals(Quoting.DOUBLE_QUOTED, shell.getCommands().get(0).getElement().getQuoting());
        assertEquals("", shell.getCommands().get(0).getElement().getTrailing().getWhitespace());
        assertEquals("", shell.getCommands().get(0).getAfter().getWhitespace());

        assertEquals(" ", shell.getCommands().get(1).getElement().getPrefix().getWhitespace());
        assertEquals("-Command", shell.getCommands().get(1).getElement().getText());
        assertEquals(Quoting.DOUBLE_QUOTED, shell.getCommands().get(1).getElement().getQuoting());
        assertEquals(" ", shell.getCommands().get(1).getElement().getTrailing().getWhitespace());
        assertEquals(" \t\\\n\t\t  ", shell.getCommands().get(1).getAfter().getWhitespace());


        assertEquals("", shell.getCommands().get(2).getElement().getPrefix().getWhitespace());
        assertEquals("bash", shell.getCommands().get(2).getElement().getText());
        assertEquals(Quoting.DOUBLE_QUOTED, shell.getCommands().get(2).getElement().getQuoting());
        assertEquals("", shell.getCommands().get(2).getElement().getTrailing().getWhitespace());
        assertEquals("", shell.getCommands().get(2).getAfter().getWhitespace());

        assertEquals(" ", shell.getCommands().get(3).getElement().getPrefix().getWhitespace());
        assertEquals("-c", shell.getCommands().get(3).getElement().getText());
        assertEquals(Quoting.DOUBLE_QUOTED, shell.getCommands().get(3).getElement().getQuoting());
        assertEquals("   ", shell.getCommands().get(3).getElement().getTrailing().getWhitespace());
        assertEquals("", shell.getCommands().get(3).getAfter().getWhitespace());
        assertEquals(" ", shell.getExecFormPrefix().getWhitespace());
    }

    @Test
    void testUserOnly() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("USER root".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.User user = (Docker.User) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, user.getPrefix());
        assertEquals("root", user.getUsername().getText());
        assertNull(user.getGroup());
    }

    @Test
    void testUserWithGroup() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("USER root:admin".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.User user = (Docker.User) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, user.getPrefix());
        assertEquals("root", user.getUsername().getText());
        assertEquals(" ", user.getUsername().getPrefix().getWhitespace());
        assertEquals("admin", user.getGroup().getText());
        assertEquals("", user.getGroup().getTrailing().getWhitespace());
    }

    @Test
    void testUserWithFunkySpacing() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("USER    root:admin   \t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.User user = (Docker.User) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, user.getPrefix());
        assertEquals("root", user.getUsername().getText());
        assertEquals("    ", user.getUsername().getPrefix().getWhitespace());
        assertEquals("admin", user.getGroup().getText());
        assertEquals("   \t", user.getGroup().getTrailing().getWhitespace());
    }

    @Test
    void testVolumeShellFormat(){
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("VOLUME [ \"/var/log\", \"/var/log2\" ]\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Volume volume = (Docker.Volume) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, volume.getPrefix());
        assertEquals(" ", volume.getExecFormPrefix().getWhitespace());
        assertEquals("\t", volume.getExecFormSuffix().getWhitespace());

        List<DockerRightPadded<Docker.Literal>> args = volume.getPaths();
        assertEquals(2, args.size());
        assertEquals("/var/log", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("", args.get(0).getAfter().getWhitespace());

        assertEquals("/var/log2", args.get(1).getElement().getText());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals("", args.get(1).getAfter().getWhitespace());
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
        assertEquals("/var/log", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("", args.get(0).getAfter().getWhitespace());

        assertEquals("/var/log2", args.get(1).getElement().getText());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals("\t", args.get(1).getAfter().getWhitespace());
    }

    @Test
    void testWorkDir() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("WORKDIR /var/log\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Workdir workdir = (Docker.Workdir) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, workdir.getPrefix());
        assertEquals("/var/log", workdir.getPath().getText());
        assertEquals(" ", workdir.getPath().getPrefix().getWhitespace());
        assertEquals("", workdir.getPath().getTrailing().getWhitespace()); // TODO: FIX missing traililng here.
    }

    @Test
    void testLabelSingle() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("LABEL foo=bar".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Label label = (Docker.Label) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, label.getPrefix());
        assertEquals("foo", label.getArgs().get(0).getElement().getKey());
        assertEquals(" ", label.getArgs().get(0).getElement().getPrefix().getWhitespace());
        assertTrue(label.getArgs().get(0).getElement().isHasEquals());
        assertEquals("bar", label.getArgs().get(0).getElement().getValue());
        assertEquals(Quoting.UNQUOTED, label.getArgs().get(0).getElement().getQuoting());
    }

    @Test
    void testLabelMultiple() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("LABEL foo=bar baz=qux".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Label label = (Docker.Label) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, label.getPrefix());
        assertEquals("foo", label.getArgs().get(0).getElement().getKey());
        assertEquals(" ", label.getArgs().get(0).getElement().getPrefix().getWhitespace());
        assertTrue(label.getArgs().get(0).getElement().isHasEquals());
        assertEquals("bar", label.getArgs().get(0).getElement().getValue());
        assertEquals(Quoting.UNQUOTED, label.getArgs().get(0).getElement().getQuoting());

        assertEquals("baz", label.getArgs().get(1).getElement().getKey());
        assertEquals(" ", label.getArgs().get(1).getElement().getPrefix().getWhitespace());
        assertTrue(label.getArgs().get(1).getElement().isHasEquals());
        assertEquals("qux", label.getArgs().get(1).getElement().getValue());
        assertEquals(Quoting.UNQUOTED, label.getArgs().get(1).getElement().getQuoting());
    }

    @Test
    void testLabelMultiline() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("LABEL foo=bar \\\n\t\tbaz=qux \\\n\t\tquux=\"Hello World\"".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Label label = (Docker.Label) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, label.getPrefix());
        assertEquals("foo", label.getArgs().get(0).getElement().getKey());
        assertEquals(" ", label.getArgs().get(0).getElement().getPrefix().getWhitespace());
        assertTrue(label.getArgs().get(0).getElement().isHasEquals());
        assertEquals("bar", label.getArgs().get(0).getElement().getValue());
        assertEquals(Quoting.UNQUOTED, label.getArgs().get(0).getElement().getQuoting());
        assertEquals(" \\\n", label.getArgs().get(0).getAfter().getWhitespace());

        assertEquals("baz", label.getArgs().get(1).getElement().getKey());
        assertEquals("\t\t", label.getArgs().get(1).getElement().getPrefix().getWhitespace());
        assertTrue(label.getArgs().get(1).getElement().isHasEquals());
        assertEquals("qux", label.getArgs().get(1).getElement().getValue());
        assertEquals(Quoting.UNQUOTED, label.getArgs().get(1).getElement().getQuoting());
        assertEquals(" \\\n", label.getArgs().get(1).getAfter().getWhitespace());

        assertEquals("quux", label.getArgs().get(2).getElement().getKey());
        assertEquals("\t\t", label.getArgs().get(2).getElement().getPrefix().getWhitespace());
        assertTrue(label.getArgs().get(2).getElement().isHasEquals());
        assertEquals("Hello World", label.getArgs().get(2).getElement().getValue());
        assertEquals(Quoting.DOUBLE_QUOTED, label.getArgs().get(2).getElement().getQuoting());
    }

    @Test
    void testStopSignal() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("STOPSIGNAL SIGKILL".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.StopSignal stopSignal = (Docker.StopSignal) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, stopSignal.getPrefix());
        assertEquals("SIGKILL", stopSignal.getSignal().getText());
        assertEquals(" ", stopSignal.getSignal().getPrefix().getWhitespace());
    }

    @Test
    void testHealthCheckNone() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("HEALTHCHECK NONE".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);

        Docker.Healthcheck healthCheck = (Docker.Healthcheck) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, healthCheck.getPrefix());
        assertEquals(1, healthCheck.getCommands().size());
        assertEquals("NONE", healthCheck.getCommands().get(0).getElement().getText());
        assertEquals(" ", healthCheck.getCommands().get(0).getElement().getPrefix().getWhitespace());
    }

    @Test
    void testHealthCheckWithCmd() {
        DockerfileParser parser = new DockerfileParser();
        Docker.Document doc = parser.parse(new ByteArrayInputStream("HEALTHCHECK --interval=5m --timeout=3s \\\n  CMD curl -f http://localhost/ || exit 1".getBytes(StandardCharsets.UTF_8)));
        Docker.Stage stage = assertSingleStageWithChildCount(doc, 1);
        Docker.Healthcheck healthCheck = (Docker.Healthcheck) stage.getChildren().get(0);
        assertEquals(" ", healthCheck.getPrefix().getWhitespace());
        assertEquals(7, healthCheck.getCommands().size());

        assertEquals(" ", healthCheck.getOptions().get(0).getElement().getPrefix().getWhitespace());
        assertEquals("--interval", healthCheck.getOptions().get(0).getElement().getKey());
        assertEquals("5m", healthCheck.getOptions().get(0).getElement().getValue());
        assertTrue(healthCheck.getOptions().get(0).getElement().isHasEquals());
        assertEquals("", healthCheck.getOptions().get(0).getAfter().getWhitespace());

        assertEquals(" ", healthCheck.getOptions().get(1).getElement().getPrefix().getWhitespace());
        assertEquals("--timeout", healthCheck.getOptions().get(1).getElement().getKey());
        assertEquals("3s", healthCheck.getOptions().get(1).getElement().getValue());
        assertTrue(healthCheck.getOptions().get(1).getElement().isHasEquals());
        assertEquals(" \\\n", healthCheck.getOptions().get(1).getAfter().getWhitespace());

        assertEquals("", healthCheck.getCommands().get(0).getAfter().getWhitespace());
        assertEquals("CMD", healthCheck.getCommands().get(0).getElement().getText());
        assertEquals("  ", healthCheck.getCommands().get(0).getElement().getPrefix().getWhitespace());
        assertEquals("", healthCheck.getCommands().get(0).getAfter().getWhitespace());

        assertEquals("curl", healthCheck.getCommands().get(1).getElement().getText());
        assertEquals(" ", healthCheck.getCommands().get(1).getElement().getPrefix().getWhitespace());
        assertEquals("-f", healthCheck.getCommands().get(2).getElement().getText());
        assertEquals(" ", healthCheck.getCommands().get(2).getElement().getPrefix().getWhitespace());
        assertEquals("http://localhost/", healthCheck.getCommands().get(3).getElement().getText());
        assertEquals(" ", healthCheck.getCommands().get(3).getElement().getPrefix().getWhitespace());
        assertEquals("||", healthCheck.getCommands().get(4).getElement().getText());
        assertEquals(" ", healthCheck.getCommands().get(4).getElement().getPrefix().getWhitespace());
        assertEquals("exit", healthCheck.getCommands().get(5).getElement().getText());
        assertEquals(" ", healthCheck.getCommands().get(5).getElement().getPrefix().getWhitespace());
        assertEquals("1", healthCheck.getCommands().get(6).getElement().getText());
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
        assertEquals("foo.txt", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(0).getAfter().getWhitespace());

        assertEquals("bar.txt", args.get(1).getElement().getText());
        assertEquals("", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(1).getAfter().getWhitespace());

        assertEquals("baz.txt", args.get(2).getElement().getText());
        assertEquals("", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(2).getAfter().getWhitespace());

        assertEquals("qux.txt", args.get(3).getElement().getText());
        assertEquals("", args.get(3).getElement().getPrefix().getWhitespace());
        assertEquals("", args.get(3).getAfter().getWhitespace());

        DockerRightPadded<Docker.Literal> dest = add.getDestination();
        assertEquals("/tmp/", dest.getElement().getText());
        assertEquals(" ", dest.getElement().getPrefix().getWhitespace());
        assertEquals("\t", dest.getAfter().getWhitespace());
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
        assertEquals("foo.txt", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(0).getAfter().getWhitespace());

        assertEquals("bar.txt", args.get(1).getElement().getText());
        assertEquals("", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(1).getAfter().getWhitespace());

        assertEquals("baz.txt", args.get(2).getElement().getText());
        assertEquals("", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(2).getAfter().getWhitespace());

        assertEquals("qux.txt", args.get(3).getElement().getText());
        assertEquals("", args.get(3).getElement().getPrefix().getWhitespace());
        assertEquals("", args.get(3).getAfter().getWhitespace());

        DockerRightPadded<Docker.Literal> dest = add.getDestination();
        assertEquals("/tmp/", dest.getElement().getText());
        assertEquals(" ", dest.getElement().getPrefix().getWhitespace());
        assertEquals("\t", dest.getAfter().getWhitespace());

        List<DockerRightPadded<Docker.Option>> options = add.getOptions();
        assertEquals(3, options.size());

        DockerRightPadded<Docker.Option> option = options.get(0);

        assertEquals(" ", option.getElement().getPrefix().getWhitespace());
        assertEquals("--chown", option.getElement().getKeyArgs().getKey());
        assertEquals("foo:bar", option.getElement().getKeyArgs().getValue());
        assertTrue(option.getElement().getKeyArgs().isHasEquals());
        assertEquals("", option.getAfter().getWhitespace());

        option = options.get(1);
        assertEquals(" ", option.getElement().getPrefix().getWhitespace());
        assertEquals("--keep-git-dir", option.getElement().getKeyArgs().getKey());
        assertNull(option.getElement().getKeyArgs().getValue());
        assertFalse(option.getElement().getKeyArgs().isHasEquals());
        assertEquals("", option.getAfter().getWhitespace());

        option = options.get(2);
        assertEquals(" ", option.getElement().getPrefix().getWhitespace());
        assertEquals("--checksum", option.getElement().getKeyArgs().getKey());
        assertEquals("sha256:24454f830c", option.getElement().getKeyArgs().getValue());
        assertTrue(option.getElement().getKeyArgs().isHasEquals());
        assertEquals("", option.getAfter().getWhitespace());
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
        assertEquals("foo.txt", args.get(0).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(0).getAfter().getWhitespace());

        assertEquals("bar.txt", args.get(1).getElement().getText());
        assertEquals("", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(1).getAfter().getWhitespace());

        assertEquals("baz.txt", args.get(2).getElement().getText());
        assertEquals("", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals(" \\\n\t\t", args.get(2).getAfter().getWhitespace());

        assertEquals("qux.txt", args.get(3).getElement().getText());
        assertEquals("", args.get(3).getElement().getPrefix().getWhitespace());
        assertEquals("", args.get(3).getAfter().getWhitespace());

        DockerRightPadded<Docker.Literal> dest = copy.getDestination();
        assertEquals("/tmp/", dest.getElement().getText());
        assertEquals(" ", dest.getElement().getPrefix().getWhitespace());
        assertEquals("\t", dest.getAfter().getWhitespace());
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
        assertEquals("echo", args.get(0).getElement().getText());
        assertEquals("Hello", args.get(1).getElement().getText());
        assertEquals("World", args.get(2).getElement().getText());
        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals(" ", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("\t", args.get(2).getAfter().getWhitespace());
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
        assertEquals(" ", opts.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("--mount", opts.get(0).getElement().getKeyArgs().getKey());
        assertEquals("type=cache,target=/var/cache/apt,sharing=locked", opts.get(0).getElement().getKeyArgs().getValue());
        assertTrue(opts.get(0).getElement().getKeyArgs().isHasEquals());
        assertEquals(" \\\n", opts.get(0).getAfter().getWhitespace());
        assertEquals("  ", opts.get(1).getElement().getPrefix().getWhitespace());
        assertEquals("--mount", opts.get(1).getElement().getKeyArgs().getKey());
        assertEquals("type=cache,target=/var/lib/apt,sharing=locked", opts.get(1).getElement().getKeyArgs().getValue());
        assertTrue(opts.get(1).getElement().getKeyArgs().isHasEquals());
        assertEquals(" \\\n", opts.get(1).getAfter().getWhitespace());

        List<DockerRightPadded<Docker.Literal>> args = cmd.getCommands();
        assertEquals(8, args.size());

        assertEquals("  ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("apt", args.get(0).getElement().getText());
        assertEquals("", args.get(0).getAfter().getWhitespace());

        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals("update", args.get(1).getElement().getText());
        assertEquals("", args.get(1).getAfter().getWhitespace());

        assertEquals(" ", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("&&", args.get(2).getElement().getText());
        assertEquals("", args.get(2).getAfter().getWhitespace());

        assertEquals(" ", args.get(3).getElement().getPrefix().getWhitespace());
        assertEquals("apt-get", args.get(3).getElement().getText());
        assertEquals("", args.get(3).getAfter().getWhitespace());

        assertEquals(" ", args.get(4).getElement().getPrefix().getWhitespace());
        assertEquals("--no-install-recommends", args.get(4).getElement().getText());
        assertEquals("", args.get(4).getElement().getTrailing().getWhitespace());
        assertEquals("", args.get(4).getAfter().getWhitespace());

        assertEquals(" ", args.get(5).getElement().getPrefix().getWhitespace());
        assertEquals("install", args.get(5).getElement().getText());
        assertEquals("", args.get(5).getElement().getTrailing().getWhitespace());
        assertEquals("", args.get(5).getAfter().getWhitespace());

        assertEquals(" ", args.get(6).getElement().getPrefix().getWhitespace());
        assertEquals("-y", args.get(6).getElement().getText());
        assertEquals("", args.get(6).getElement().getTrailing().getWhitespace());
        assertEquals("", args.get(6).getAfter().getWhitespace());

        assertEquals(" ", args.get(7).getElement().getPrefix().getWhitespace());
        assertEquals("gcc", args.get(7).getElement().getText());
        assertEquals("", args.get(7).getAfter().getWhitespace());
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
        assertEquals(14, args.size());

        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("<<EOF", args.get(0).getElement().getText());
        assertEquals("", args.get(0).getElement().getTrailing().getWhitespace());

        assertEquals("\n", args.get(0).getAfter().getWhitespace());

        assertEquals("", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals("apt-get", args.get(1).getElement().getText());

        assertEquals(" ", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("update", args.get(2).getElement().getText());
        assertEquals("\n", args.get(2).getAfter().getWhitespace());

        assertEquals("", args.get(3).getElement().getPrefix().getWhitespace());
        assertEquals("apt-get", args.get(3).getElement().getText());
        assertEquals(" ", args.get(4).getElement().getPrefix().getWhitespace());
        assertEquals("install", args.get(4).getElement().getText());

        assertEquals(" ", args.get(5).getElement().getPrefix().getWhitespace());
        assertEquals("-y", args.get(5).getElement().getText());
        assertEquals("curl", args.get(6).getElement().getText());
    }

    @Test
    public void testRunWithHeredocAfterOtherCommands() {
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
        assertEquals(20, args.size());

        assertEquals(" ", args.get(0).getElement().getPrefix().getWhitespace());
        assertEquals("echo", args.get(0).getElement().getText());

        assertEquals(" ", args.get(1).getElement().getPrefix().getWhitespace());
        assertEquals("Hello", args.get(1).getElement().getText());

        assertEquals(" ", args.get(2).getElement().getPrefix().getWhitespace());
        assertEquals("World", args.get(2).getElement().getText());
        assertEquals("", args.get(2).getAfter().getWhitespace());

        assertEquals(" ", args.get(3).getElement().getPrefix().getWhitespace());
        assertEquals("<<EOF", args.get(3).getElement().getText());
        assertEquals("\n", args.get(3).getAfter().getWhitespace());

        assertEquals("", args.get(4).getElement().getPrefix().getWhitespace());
        assertEquals("apt-get", args.get(4).getElement().getText());
        assertEquals("", args.get(4).getElement().getTrailing().getWhitespace());

        assertEquals(" ", args.get(5).getElement().getPrefix().getWhitespace());
        assertEquals("update", args.get(5).getElement().getText());
        assertEquals("", args.get(5).getElement().getTrailing().getWhitespace());

        assertEquals("\n", args.get(5).getAfter().getWhitespace());

        assertEquals("", args.get(6).getElement().getPrefix().getWhitespace());
        assertEquals("apt-get", args.get(6).getElement().getText());
        assertEquals(" ", args.get(7).getElement().getPrefix().getWhitespace());

        assertEquals(" ", args.get(7).getElement().getPrefix().getWhitespace());
        assertEquals("install", args.get(7).getElement().getText());
        assertEquals("-y", args.get(8).getElement().getText());
        assertEquals(" ", args.get(8).getElement().getPrefix().getWhitespace());
    }
}
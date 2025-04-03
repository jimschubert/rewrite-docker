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
import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerParserTest {
    @Test
    void testCommentSingleWithTrailingSpace() {
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("# This is a comment\t\t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

        Docker.Comment comment = (Docker.Comment) stage.getChildren().get(0);
        assertComment(comment, "This is a comment", " ", "\t\t");
    }

    @Test
    void testDirective() {
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("#    syntax=docker/dockerfile:1".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

        Docker.Directive directive = (Docker.Directive) stage.getChildren().get(0);
        assertDirective(directive, "syntax", true, "docker/dockerfile:1", "    ", "");
    }

    @Test
    void testArgNoAssignment() {
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ARG foo".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

        Docker.Arg arg = (Docker.Arg) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, arg.getPrefix());
        List<DockerRightPadded<Docker.KeyArgs>> args = arg.getArgs();

        assertArg(args.get(0), "foo", false, null, " ", "", Quoting.UNQUOTED);
    }

    @Test
    void testArgComplex() {
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ARG foo=bar baz MY_VAR OTHER_VAR=\"some default\" \t".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ARG foo=bar baz MY_VAR \\\nOTHER_VAR=\"some default\" \t\\\n\t\tLAST".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("CMD [ \"echo\", \"Hello World\" ]   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("CMD echo Hello World   ".getBytes(StandardCharsets.UTF_8)));
        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);
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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("CMD \"echo Hello World\"   ".getBytes(StandardCharsets.UTF_8)));
        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);
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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("CMD echo Hello World   ".getBytes(StandardCharsets.UTF_8)));
        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);
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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT [ \"echo\", \"Hello World\" ]   ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT echo Hello World   ".getBytes(StandardCharsets.UTF_8)));
        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);
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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ENTRYPOINT \"echo Hello World\"   ".getBytes(StandardCharsets.UTF_8)));
        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);
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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ENV MY_NAME=\"John Doe\" MY_DOG=Rex\\ The\\ Dog MY_CAT=fluffy".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

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
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ENV MY_NAME=\"John Doe\" MY_DOG=Rex\\ The\\ Dog \\\nMY_CAT=fluffy ".getBytes(StandardCharsets.UTF_8)));

        Docker.Stage stage = assertSingleStageWithChildCount((Docker.Document) doc, 1);

        Docker.Env env = (Docker.Env) stage.getChildren().get(0);
        assertEquals(Space.EMPTY, env.getPrefix());

        List<DockerRightPadded<Docker.KeyArgs>> args = env.getArgs();
        assertEquals(3, args.size());

        assertArg(args.get(0), "MY_NAME", true, "John Doe", " ", "", Quoting.DOUBLE_QUOTED);
        assertArg(args.get(1), "MY_DOG", true, "Rex The Dog", " ", " \\\n", Quoting.UNQUOTED);
        assertArg(args.get(2), "MY_CAT", true, "fluffy", "", " ", Quoting.UNQUOTED);
    }
}
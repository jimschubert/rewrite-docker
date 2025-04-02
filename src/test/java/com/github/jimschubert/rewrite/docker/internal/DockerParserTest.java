package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DockerParserTest {
    @Test
    void testParseSingleCommentWithTrailingSpace() {
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("# This is a comment\t\t".getBytes(StandardCharsets.UTF_8)));
        assertNotNull(doc);
        assertInstanceOf(Docker.Document.class, doc);

        Docker.Document document = (Docker.Document) doc;
        assertNotNull(document.getStages());
        assertEquals(1, document.getStages().size());

        Docker.Stage stage = document.getStages().get(0);
        assertNotNull(stage.getChildren());
        assertEquals(1, stage.getChildren().size());

        Docker.Comment comment = (Docker.Comment) stage.getChildren().get(0);
        DockerRightPadded<Docker.Literal> value = comment.getText();

        assertNotNull(value.getElement());
        assertEquals("\t\t", value.getAfter().getWhitespace());

        assertEquals("This is a comment", value.getElement().getText());
        assertEquals(" ", value.getElement().getPrefix().getWhitespace());
    }


    @Test
    void testParseDirective() {
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("#    syntax=docker/dockerfile:1".getBytes(StandardCharsets.UTF_8)));
        assertNotNull(doc);
        assertInstanceOf(Docker.Document.class, doc);

        Docker.Document document = (Docker.Document) doc;
        assertNotNull(document.getStages());
        assertEquals(1, document.getStages().size());

        Docker.Stage stage = document.getStages().get(0);
        assertNotNull(stage.getChildren());
        assertEquals(1, stage.getChildren().size());

        Docker.Directive directive = (Docker.Directive) stage.getChildren().get(0);
        DockerRightPadded<Docker.KeyArgs> value = directive.getDirective();

        assertNotNull(value.getElement());
        assertEquals("", value.getAfter().getWhitespace());

        assertEquals("syntax", value.getElement().getKey());
        assertTrue(value.getElement().isHasEquals());
        assertEquals("docker/dockerfile:1", value.getElement().getValue());

        assertEquals("    ", value.getElement().getPrefix().getWhitespace());
    }

    @Test
    void testArgDirectiveNoAssignment() {
        DockerParser parser = new DockerParser();
        Docker doc = parser.parse(new ByteArrayInputStream("ARG foo".getBytes(StandardCharsets.UTF_8)));
        assertNotNull(doc);
        assertInstanceOf(Docker.Document.class, doc);

        Docker.Document document = (Docker.Document) doc;
        assertNotNull(document.getStages());
        assertEquals(1, document.getStages().size());

        Docker.Stage stage = document.getStages().get(0);
        assertNotNull(stage.getChildren());
        assertEquals(1, stage.getChildren().size());

        Docker.Arg arg = (Docker.Arg) stage.getChildren().get(0);
        DockerRightPadded<Docker.KeyArgs> value = arg.getArgs().get(0);

        assertNotNull(value.getElement());
        assertEquals("", value.getAfter().getWhitespace());

        assertEquals("foo", value.getElement().getKey());
        assertFalse(value.getElement().isHasEquals());
        assertNull(value.getElement().getValue());

        assertEquals(" ", value.getElement().getPrefix().getWhitespace());
    }
}
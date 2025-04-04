package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Quoting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DockerParserHelpers {

    public static Docker.Stage assertSingleStageWithChildCount(Docker.Document document, int expectedStageCount) {
        assertNotNull(document);
        assertNotNull(document.getStages());
        assertEquals(1, document.getStages().size());

        Docker.Stage stage = document.getStages().get(0);
        assertNotNull(stage.getChildren());
        assertEquals(expectedStageCount, stage.getChildren().size());
        return stage;
    }

    public static void assertComment(Docker.Comment comment, String expectedText, String expectedPrefix, String expectedAfter) {
        DockerRightPadded<Docker.Literal> value = comment.getText();

        assertNotNull(value.getElement());
        assertEquals(expectedAfter, value.getAfter().getWhitespace());
        assertEquals(expectedText, value.getElement().getText());
        assertEquals(expectedPrefix, value.getElement().getPrefix().getWhitespace());
    }

    public static void assertDirective(Docker.Directive directive, String expectedKey, boolean hasEquals, String expectedValue, String expectedPrefix, String expectedAfter) {
        DockerRightPadded<Docker.KeyArgs> value = directive.getDirective();

        assertNotNull(value.getElement());
        assertEquals(expectedAfter, value.getAfter().getWhitespace());
        assertEquals(expectedKey, value.getElement().getKey());
        assertEquals(hasEquals, value.getElement().isHasEquals());
        assertEquals(expectedValue, value.getElement().getValue());
        assertEquals(expectedPrefix, value.getElement().getPrefix().getWhitespace());
    }

    public static void assertArg(
            DockerRightPadded<Docker.KeyArgs> value,
            String expectedKey,
            boolean hasEquals,
            String expectedValue,
            String expectedPrefix,
            String expectedAfter,
            Quoting expectedQuoting) {
        assertNotNull(value.getElement());
        assertEquals(expectedAfter, value.getAfter().getWhitespace());
        assertEquals(expectedKey, value.getElement().getKey());
        assertEquals(hasEquals, value.getElement().isHasEquals());
        assertEquals(expectedValue, value.getElement().getValue());
        assertEquals(expectedPrefix, value.getElement().getPrefix().getWhitespace());
        assertEquals(expectedQuoting, value.getElement().getQuoting());
    }
}

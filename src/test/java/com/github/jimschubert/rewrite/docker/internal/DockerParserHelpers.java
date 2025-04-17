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

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Quoting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DockerParserHelpers {

    public static Docker.Stage assertSingleStageWithChildCount(Docker.Document document, int expectedStageChildCount) {
        return assertMultiStageWithChildCount(document, 1, expectedStageChildCount);
    }

    public static Docker.Stage assertMultiStageWithChildCount(Docker.Document document, int expectedStageCount, int expectedStageChildCount) {
        assertNotNull(document,
                "Expected document to be non-null but was null");
        assertNotNull(document.getStages(),
                "Expected document to have stages but was null");
        assertEquals(expectedStageCount, document.getStages().size(),
                "Expected document to have " + expectedStageCount + " stage but was " + document.getStages().size());

        Docker.Stage stage = document.getStages().get(0);
        assertNotNull(stage.getChildren(),
                "Expected stage to have children but was null");
        assertEquals(expectedStageChildCount, stage.getChildren().size(),
                "Expected stage to have " + expectedStageChildCount + " children but was " + stage.getChildren().size());
        return stage;
    }

    public static void assertComment(Docker.Comment comment, String expectedPrefix, String expectedText, String expectedAfter) {
        DockerRightPadded<Docker.Literal> value = comment.getText();

        assertNotNull(value.getElement(),
                "Expected Comment to have an element but was null");
        assertEquals(expectedAfter, value.getAfter().getWhitespace(),
                "Expected Comment to have trailing whitespace '" + printableWhiteSpace(expectedAfter) + "' but was '" + printableWhiteSpace(value.getAfter().getWhitespace()) + "'");
        assertEquals(expectedText, value.getElement().getText(),
                "Expected Comment text to be '" + expectedText + "' but was '" + value.getElement().getText() + "'");
        assertEquals(expectedPrefix, value.getElement().getPrefix().getWhitespace(),
                "Expected Comment prefix whitespace to be '" + printableWhiteSpace(expectedPrefix) + "' but was '" + printableWhiteSpace(value.getElement().getPrefix().getWhitespace()) + "'");
    }

    public static void assertDirective(Docker.Directive directive, String expectedPrefix, String expectedKey, boolean hasEquals, String expectedValue, String expectedAfter) {
        DockerRightPadded<Docker.KeyArgs> value = directive.getDirective();

        assertNotNull(value.getElement(),
                "Expected Directive to have an element but was null");
        assertEquals(expectedAfter, value.getAfter().getWhitespace(),
                "Expected Directive to have trailing whitespace '" + printableWhiteSpace(expectedAfter) + "' but was '" + printableWhiteSpace(value.getAfter().getWhitespace()) + "'");
        assertEquals(expectedKey, value.getElement().key(),
                "Expected Directive key to be '" + expectedKey + "' but was '" + value.getElement().key() + "'");
        assertEquals(hasEquals, value.getElement().isHasEquals(),
                "Expected Directive hasEquals to be '" + hasEquals + "' but was '" + value.getElement().isHasEquals() + "'");
        assertEquals(expectedValue, value.getElement().value(),
                "Expected Directive value to be '" + expectedValue + "' but was '" + value.getElement().value() + "'");
        assertEquals(expectedPrefix, value.getElement().getPrefix().getWhitespace(),
                "Expected Directive prefix whitespace to be '" + printableWhiteSpace(expectedPrefix) + "' but was '" + printableWhiteSpace(value.getElement().getPrefix().getWhitespace()) + "'");
    }

    public static void assertRightPaddedArg(
            DockerRightPadded<Docker.KeyArgs> value,
            Quoting expectedQuoting,
            String expectedPrefix,
            String expectedKey,
            boolean hasEquals,
            String expectedValue,
            String expectedAfter) {
        assertNotNull(value.getElement(),
                "Expected KeyArgs to have an element but was null");

        assertKeyArgs(value.getElement(), expectedQuoting, expectedPrefix, expectedKey, hasEquals, expectedValue);

        assertEquals(expectedAfter, value.getAfter().getWhitespace(),
                "Expected KeyArgs to have trailing whitespace '" + printableWhiteSpace(expectedAfter) + "' but was '" + printableWhiteSpace(value.getAfter().getWhitespace()) + "'");
    }

    public static void assertKeyArgs(Docker.KeyArgs value, Quoting expectedQuoting, String expectedPrefix, String expectedKey, boolean hasEquals, String expectedValue) {
        assertNotNull(value, "Expected KeyArgs to have an element but was null");

        assertEquals(expectedPrefix, value.getPrefix().getWhitespace(),
                "Expected KeyArgs prefix whitespace to be '" + printableWhiteSpace(expectedPrefix) + "' but was '" + (value.getPrefix().getWhitespace()) + "'");

        assertEquals(expectedKey, value.key(),
                "Expected KeyArgs key to be '" + expectedKey + "' but was '" + value.key() + "'");
        assertEquals(hasEquals, value.isHasEquals(),
                "Expected KeyArgs hasEquals to be '" + hasEquals + "' but was '" + value.isHasEquals() + "'");
        assertEquals(expectedValue, value.value(),
                "Expected KeyArgs value to be '" + expectedValue + "' but was '" + value.value() + "'");
        assertEquals(expectedQuoting, value.getQuoting(),
                "Expected KeyArgs quoting to be '" + expectedQuoting + "' but was '" + value.getQuoting() + "'");
    }

    public static void assertRightPaddedLiteral(
            DockerRightPadded<Docker.Literal> value,
            Quoting expectedQuoting, String expectedPrefix,
            String expectedText,
            String expectedTrailing,
            String expectedAfter) {
        assertNotNull(value.getElement(),
                "Expected DockerRightPadded to have an element but was null");
        assertLiteral(value.getElement(), expectedQuoting, expectedPrefix, expectedText, expectedTrailing);
        assertEquals(expectedAfter, value.getAfter().getWhitespace(),
                "Expected DockerRightPadded to have trailing whitespace '" + printableWhiteSpace(expectedAfter) + "' but was '" + printableWhiteSpace(value.getAfter().getWhitespace()) + "'");
    }

    public static void assertRightPaddedOption(
            DockerRightPadded<Docker.Option> value,
            Quoting expectedQuoting,
            String expectedPrefix,
            String expectedKey,
            boolean expectedEqualsSign,
            String expectedValue,
            String expectedAfter) {
        assertNotNull(value.getElement(),
                "Expected DockerRightPadded to have an element but was null");
        assertOption(value.getElement(), expectedQuoting, expectedPrefix, expectedKey, expectedEqualsSign, expectedValue);
        assertEquals(expectedAfter, value.getAfter().getWhitespace(),
                "Expected DockerRightPadded to have trailing whitespace '" + printableWhiteSpace(expectedAfter) + "' but was '" + printableWhiteSpace(value.getAfter().getWhitespace()) + "'");
    }

    public static void assertLiteral(
            Docker.Literal value,
            Quoting expectedQuoting, String expectedPrefix,
            String expectedText,
            String expectedTrailing) {
        assertNotNull(value);
        assertEquals(expectedText, value.getText(),
                "Expected Literal text to be '" + expectedText + "' but was '" + value.getText() + "'");
        assertEquals(expectedPrefix, value.getPrefix().getWhitespace(),
                "Expected Literal prefix whitespace to be '" + printableWhiteSpace(expectedPrefix) + "' but was '" + printableWhiteSpace(value.getPrefix().getWhitespace()) + "'");
        assertEquals(expectedQuoting, value.getQuoting(),
                "Expected Literal quoting to be '" + expectedQuoting + "' but was '" + value.getQuoting() + "'");
        assertEquals(expectedTrailing, value.getTrailing().getWhitespace(),
                "Expected Literal trailing whitespace to be '" + expectedTrailing + "' but was '" + value.getTrailing() + "'");
    }

    public static void assertOption(
            Docker.Option value,
            Quoting expectedQuoting, String expectedPrefix,
            String expectedKey,
            boolean expectedEqualsSign,
            String expectedValue) {
        assertEquals(expectedPrefix, value.getPrefix().getWhitespace(),
                "Expected Option prefix whitespace to be '" + printableWhiteSpace(expectedPrefix) + "' but was '" + printableWhiteSpace(value.getPrefix().getWhitespace()) + "'");
        assertKeyArgs(value.getKeyArgs(), expectedQuoting, "", expectedKey, expectedEqualsSign, expectedValue);
    }

    public static String printableWhiteSpace(String whitespace) {
        return whitespace.replaceAll(" ", "·").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "→");
    }
}

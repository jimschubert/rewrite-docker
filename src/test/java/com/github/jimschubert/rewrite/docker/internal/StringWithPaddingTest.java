package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Space;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringWithPaddingTest {

    static Stream<TestCase> provideTestCases() {
        return Stream.of(
                new TestCase("  content  ", "content", "  ", "  "),
                new TestCase("\tcontent\t", "content", "\t", "\t"),
                new TestCase("content", "content", "", ""),
                new TestCase("  content", "content", "  ", ""),
                new TestCase("content  ", "content", "", "  "),
                new TestCase("", "", "", ""),
                new TestCase("  ", "", "  ", "")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    @DisplayName("Test StringWithPadding.of() with various inputs")
    void testStringWithPadding(TestCase testCase) {
        StringWithPadding result = StringWithPadding.of(testCase.input);

        assertEquals(testCase.expectedContent, result.content());
        assertEquals(Space.build(testCase.expectedPrefix), result.prefix());
        assertEquals(Space.build(testCase.expectedSuffix), result.suffix());
    }

    static class TestCase {
        String input;
        String expectedContent;
        String expectedPrefix;
        String expectedSuffix;

        TestCase(String input, String expectedContent, String expectedPrefix, String expectedSuffix) {
            this.input = input;
            this.expectedContent = expectedContent;
            this.expectedPrefix = expectedPrefix;
            this.expectedSuffix = expectedSuffix;
        }
    }
}
package com.github.jimschubert.rewrite.docker.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FullLineIteratorTest {

    private FullLineIterator iterator;
    private InputStream inputStream;

    @AfterEach
    void cleanup() {
        if (iterator != null) {
            iterator.close();
        }
    }

    @Nested
    class BasicFunctionality {
        @Test
        void shouldReadSingleLine() {
            // Arrange
            inputStream = new ByteArrayInputStream("Hello, world!".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("Hello, world!", iterator.next());
            assertFalse(iterator.hasNext());
        }

        @Test
        void shouldReadMultipleLines() {
            // Arrange
            inputStream = new ByteArrayInputStream("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("Line 1", iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals("Line 2", iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals("Line 3", iterator.next());
            assertFalse(iterator.hasNext());
        }

        @Test
        void shouldHandleEmptyInput() {
            // Arrange
            inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertFalse(iterator.hasNext());
        }
    }

    @Nested
    class EmptyLineHandling {
        @Test
        void shouldHandleSingleEmptyLine() {
            // Arrange
            inputStream = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("", iterator.next());
            assertFalse(iterator.hasNext());
            assertTrue(iterator.hasEol());
        }

        @Test
        void shouldHandleMultipleEmptyLines() {
            // Arrange
            inputStream = new ByteArrayInputStream("\n\n\n".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("", iterator.next());
            assertTrue(iterator.hasEol());

            assertTrue(iterator.hasNext());
            assertEquals("", iterator.next());
            assertTrue(iterator.hasEol());

            assertTrue(iterator.hasNext());
            assertEquals("", iterator.next());
            assertTrue(iterator.hasEol());

            assertFalse(iterator.hasNext());
        }

        @Test
        void shouldHandleMixedEmptyAndNonEmptyLines() {
            // Arrange
            inputStream = new ByteArrayInputStream("Line 1\n\nLine 3\n\n".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("Line 1", iterator.next());
            assertTrue(iterator.hasEol());

            assertTrue(iterator.hasNext());
            assertEquals("", iterator.next());
            assertTrue(iterator.hasEol());

            assertTrue(iterator.hasNext());
            assertEquals("Line 3", iterator.next());
            assertTrue(iterator.hasEol());

            assertTrue(iterator.hasNext());
            assertEquals("", iterator.next());
            assertTrue(iterator.hasEol());

            assertFalse(iterator.hasNext());
        }

        @Test
        void shouldHandleLineWithoutNewline() {
            // Arrange
            inputStream = new ByteArrayInputStream("Line without newline".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("Line without newline", iterator.next());
            assertFalse(iterator.hasEol());
            assertFalse(iterator.hasNext());
        }
    }

    @Nested
    class EdgeCases {
        @Test
        void shouldHandleUnicodeCharacters() {
            // Arrange
            inputStream = new ByteArrayInputStream("こんにちは\n你好\nBonjour".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("こんにちは", iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals("你好", iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals("Bonjour", iterator.next());
            assertFalse(iterator.hasNext());
        }

        @Test
        void shouldHandleCarriageReturnProperly() {
            // Arrange - CR should be treated as a regular character, not a line delimiter
            inputStream = new ByteArrayInputStream("Line with\rcarriage return\nNext line".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("Line with\rcarriage return", iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals("Next line", iterator.next());
            assertFalse(iterator.hasNext());
        }

        @Test
        void shouldHandleCRLFAsRegularCharactersThenNewline() {
            // Arrange - CRLF should be treated as CR followed by LF, where LF is the delimiter
            inputStream = new ByteArrayInputStream("Line with\r\nCRLF".getBytes(StandardCharsets.UTF_8));

            // Act
            iterator = new FullLineIterator(inputStream);

            // Assert
            assertTrue(iterator.hasNext());
            assertEquals("Line with\r", iterator.next()); // CR is part of the string, LF is delimiter
            assertTrue(iterator.hasNext());
            assertEquals("CRLF", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Nested
    class ErrorHandling {
        @Test
        void shouldThrowWhenUsingInvalidInputStream() {
            // Arrange
            InputStream invalidStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Test exception");
                }
            };

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> new FullLineIterator(invalidStream));
        }

        @Test
        void shouldCaptureExceptionOnClose() throws IOException {
            // Arrange
            InputStream problematicStream = new ByteArrayInputStream("test data".getBytes()) {
                @Override
                public void close() throws IOException {
                    throw new IOException("Error during close");
                }
            };

            iterator = new FullLineIterator(problematicStream);

            // Act
            iterator.close();

            // Assert
            assertNotNull(iterator.exception());
            assertEquals("Error during close", iterator.exception().getMessage());
        }

        @Test
        void shouldHandleMultipleCloseCalls() {
            // Arrange
            inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            iterator = new FullLineIterator(inputStream);

            // Act & Assert - should not throw
            iterator.close();
            iterator.close(); // Second call should be safe
        }
    }

    @Nested
    class IteratorBehavior {
        @Test
        void shouldIterateThroughAllLines() {
            // Arrange
            inputStream = new ByteArrayInputStream("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8));
            iterator = new FullLineIterator(inputStream);

            // Act & Assert
            int count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }

            assertEquals(3, count);
        }

        @Test
        void shouldTrackEolForEachLine() {
            // Arrange
            inputStream = new ByteArrayInputStream("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8));
            iterator = new FullLineIterator(inputStream);

            // Act & Assert
            iterator.next();
            assertTrue(iterator.hasEol());

            iterator.next();
            assertTrue(iterator.hasEol());

            iterator.next();
            assertFalse(iterator.hasEol()); // Last line doesn't have EOL
        }
    }
}
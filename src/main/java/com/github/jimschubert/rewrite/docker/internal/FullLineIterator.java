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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * An iterator that reads lines from an InputStream up to a newline character.
 * This differs from Scanner which will splits on \r\n, \n, \r, and does not emit
 * details about the line terminator.
 */
public class FullLineIterator implements Iterator<String>, AutoCloseable {
    private final StringBuilder sb = new StringBuilder();
    private boolean closed;
    private boolean hasEol;
    private boolean nextHasEol;

    private final BufferedReader reader;
    private String nextLine;
    private IOException exception;

    public FullLineIterator(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        advance();
        hasEol = nextHasEol;
    }

    private void advance() {
        try {
            int c;
            sb.setLength(0);
            nextHasEol = false;

            while ((c = reader.read()) != -1) {
                if (c == '\n') {
                    nextHasEol = true;
                    break;
                }
                sb.append((char) c);
            }

            // If we encountered end of file and no characters were read, return null
            if (c == -1 && sb.length() == 0) {
                nextLine = null;
            } else {
                // Otherwise return the line content (even if empty)
                nextLine = sb.toString();
            }

            sb.setLength(0);
        } catch (Exception e) {
            sb.setLength(0);
            throw new IllegalArgumentException("Error reading input stream", e);
        }
    }

    public IOException exception() {
        return exception;
    }

    @Override
    public boolean hasNext() {
        return nextLine != null;
    }

    public boolean hasEol() {
        return hasEol;
    }

    @Override
    public String next() {
        String currentLine = nextLine;
        hasEol = nextHasEol;
        advance();
        return currentLine;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            reader.close();
        } catch (IOException e) {
            exception = e;
        }
        closed = true;
    }
}

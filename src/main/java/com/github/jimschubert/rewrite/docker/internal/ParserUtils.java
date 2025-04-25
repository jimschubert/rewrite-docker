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
import org.openrewrite.internal.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static com.github.jimschubert.rewrite.docker.internal.ParserConstants.*;

public class ParserUtils {
    public static Docker.Port stringToPorts(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        StringWithPadding stringWithPadding = StringWithPadding.of(s);
        String content = stringWithPadding.content();
        String[] parts = content.split("/");

        if (parts.length == 2) {
            return new Docker.Port(stringWithPadding.prefix(), parts[0], parts[1], true);
        } else if (parts.length == 1) {
            return new Docker.Port(stringWithPadding.prefix(), parts[0], "tcp", false);
        }
        return null;
    }

    public static Docker.KeyArgs stringToKeyArgs(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        StringWithPadding stringWithPadding = StringWithPadding.of(s);
        String content = stringWithPadding.content();

        @SuppressWarnings("RegExpRepeatedSpace")
        String delim = content.contains(EQUAL) ? EQUAL : SPACE;
        String[] parts = content.split(delim, EQUAL.equals(delim) ? 2 : 0);
        String key = parts.length > 0 ? parts[0] : EMPTY;
        String value = parts.length > 1 ? parts[1].trim() : null;
        Quoting q = Quoting.UNQUOTED;

        if (value != null) {
            if (value.startsWith(DOUBLE_QUOTE) && value.endsWith(DOUBLE_QUOTE)) {
                q = Quoting.DOUBLE_QUOTED;
                value = value.substring(1, value.length() - 1);
            } else if (value.startsWith(SINGLE_QUOTE) && value.endsWith(SINGLE_QUOTE)) {
                q = Quoting.SINGLE_QUOTED;
                value = value.substring(1, value.length() - 1);
            }
        }
        return new Docker.KeyArgs(stringWithPadding.prefix(), Docker.Literal.build(key), Docker.Literal.build(value), EQUAL.equals(delim), q);
    }

    public static <T> List<DockerRightPadded<T>> parseElements(String input, String delims, boolean appendRightPadding, ParserState state, Function<String, T> elementCreator) {
        List<DockerRightPadded<T>> elements = new ArrayList<>();
        StringBuilder currentElement = new StringBuilder();
        StringBuilder afterBuilder = new StringBuilder(); // queue up escaped newlines and whitespace as 'after' for previous element

        // inCollectible is used to accumulate elements within surrounding characters like single/double quotes, parentheses, etc.
        boolean inCollectible = false;
        char doubleQuote = DOUBLE_QUOTE.charAt(0);
        char singleQuote = SINGLE_QUOTE.charAt(0);
        char bracketOpen = '(';
        char bracketClose = ')';
        char braceOpen = '{';
        char braceClose = '}';
        char squareBracketOpen = '[';
        char squareBracketClose = ']';
        char escape = state.escapeChar();
        char quote = 0;
        char lastChar = 0;
        char comma = ',';

        // create a lookup of chars from delims
        HashSet<Character> delimSet = new HashSet<>();
        for (char c : delims.toCharArray()) {
            delimSet.add(c);
        }

        boolean inHeredoc = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (inCollectible) {
                if ((c == quote || c == bracketClose || c == braceClose || c == squareBracketClose) && lastChar != escape) {
                    inCollectible = false;
                }
                currentElement.append(c);
            } else {
                if (delimSet.contains(c) && (lastChar != escape || (inHeredoc && c == '\n'))) {
                    if (!StringUtils.isBlank(currentElement.toString())) {
                        elements.add(DockerRightPadded.build(elementCreator.apply(currentElement.toString()))
                                .withAfter(Space.EMPTY));
                        currentElement.setLength(0);
                    }
                    // drop comma, assuming we are creating a list of elements
                    if (c != comma) {
                        currentElement.append(c);
                    }
                } else {
                    if (c == doubleQuote || c == singleQuote || c == bracketOpen || c == braceOpen || c == squareBracketOpen) {
                        inCollectible = true;
                        quote = c;
                    }

                    Heredoc heredoc = state.heredoc();
                    if (inHeredoc && !elements.isEmpty() && elements.get(elements.size() - 1).getElement() instanceof Docker.Literal) {
                        Docker.Literal literal = (Docker.Literal) elements.get(elements.size() - 1).getElement();
                        // allows commands to come after a heredoc. Does not support heredoc within a heredoc or multiple heredocs

                        if (literal.getText() != null && heredoc != null && literal.getText().equals(heredoc.name())) {
                            inHeredoc = false;
                        }
                    }

                    // Check if within a heredoc and set escape character to '\n'
                    if (heredoc != null && c == '\n' && !inHeredoc) {
                        inHeredoc = true;
                        afterBuilder.append(c);
                        if (currentElement.length() > 0 && (
                                currentElement.toString().endsWith(heredoc.indicator()) || (heredoc.redirectionTo() != null && currentElement.toString().endsWith(heredoc.redirectionTo())))) {
                            elements.add(DockerRightPadded.build(elementCreator.apply(currentElement.toString()))
                                    .withAfter(Space.build(afterBuilder.toString())));
                            currentElement.setLength(0);
                            afterBuilder.setLength(0);
                        }

                        lastChar = c;
                        continue;
                    } else //noinspection ConstantValue
                        if (heredoc != null && c == '\n' && inHeredoc) {
                            // IntelliJ incorrectly flags inHeredoc as a constant 'true', but it's obviously not.
                            if (!currentElement.toString().endsWith(heredoc.indicator())) {
                                afterBuilder.append(c);
                                // this check allows us to accumulate "after" newlines and whitespace after for the last element
                                if (currentElement.length() > 0) {
                                    elements.add(DockerRightPadded.build(elementCreator.apply(currentElement.toString()))
                                            .withAfter(Space.build(afterBuilder.toString())));
                                    currentElement.setLength(0);
                                    afterBuilder.setLength(0);
                                }

                                lastChar = c;
                                continue;
                            }

                            // if we have a heredoc name, we are done with the heredoc
                            inHeredoc = false;
                        }

                    // "peek": if the current character is an escape and the next character is newline or carriage return, 'after' and advance
                    int nextCharIndex = i + 1;
                    if (c == escape && nextCharIndex < input.length() && (input.charAt(nextCharIndex) == '\n' || input.charAt(nextCharIndex) == '\r')) {
                        // if we had already collected some whitespace (only whitespace), add it as 'after' to the last element
                        if (StringUtils.isBlank(currentElement.toString())) {
                            afterBuilder.append(currentElement);
                            currentElement.setLength(0);
                        }

                        char next = input.charAt(nextCharIndex);
                        afterBuilder.append(escape).append(next);

                        // manually advance
                        lastChar = next;
                        i++;
                        continue;
                    }

                    // if 'after' builder is not empty and the character is whitespace, accumulate it
                    if (afterBuilder.length() > 0 && (c == ' ' || c == '\t' || c == '\n')) {
                        afterBuilder.append(c);
                        lastChar = c;
                        continue;
                    }

                    // Drop escape character if it is followed by a space
                    // other situations will retain the escape character
                    if (lastChar == escape && c == ' ') {
                        currentElement.setLength(currentElement.length() - 1);
                    }

                    // no longer accumulating a prefix, add as "after" to the last element
                    if (!elements.isEmpty() && afterBuilder.length() > 0) {
                        int idx = elements.size() - 1;
                        DockerRightPadded<T> element = elements.get(idx);
                        elements.set(idx, element.withAfter(Space.append(element.getAfter(), Space.build(afterBuilder.toString()))));
                        afterBuilder.setLength(0);
                    }

                    // Only collect the current element if we're not "in a prefix" situation
                    if (afterBuilder.length() == 0) {
                        currentElement.append(c);
                    }
                }
            }
            lastChar = c;
        }

        if (currentElement.length() > 0) {
            // if it's whitespace only, add it as "after" to the last element
            if (StringUtils.isBlank(currentElement.toString())) {
                if (!elements.isEmpty()) {
                    int idx = elements.size() - 1;
                    elements.set(idx, elements.get(idx).withAfter(Space.build(currentElement.toString())));
                }
            } else {
                DockerRightPadded<T> element = DockerRightPadded.build(elementCreator.apply(currentElement.toString()));
                if (appendRightPadding) {
                    element = element.withAfter(state.rightPadding());
                }
                elements.add(element);
            }
        }

        if (afterBuilder.length() > 0) {
            int idx = elements.size() - 1;
            if (idx >= 0) {
                DockerRightPadded<T> element = elements.get(idx);
                elements.set(idx, element.withAfter(Space.append(element.getAfter(), Space.build(afterBuilder.toString()))));
            }
        }

        return elements;
    }

    public static List<DockerRightPadded<Docker.Port>> parsePorts(String input, ParserState state) {
        return parseElements(input, SPACE + TAB, true, state, ParserUtils::stringToPorts);
    }

    public static List<DockerRightPadded<Docker.KeyArgs>> parseArgs(String input, ParserState state) {
        return parseElements(input, SPACE + TAB, true, state, ParserUtils::stringToKeyArgs);
    }

    public static List<DockerRightPadded<Docker.Literal>> parseLiterals(String input, ParserState state) {
        return parseElements(input, SPACE, true, state, ParserUtils::createLiteral);
    }

    public static List<DockerRightPadded<Docker.Literal>> parseLiterals(Form form, String input, ParserState state) {
        // appendRightPadding is true for shell form, false for exec form
        // exec form is a JSON array, so we need to parse it differently where right padding is after the ']'.
        return parseElements(input, form == Form.EXEC ? COMMA : SPACE, form == Form.SHELL, state, ParserUtils::createLiteral);
    }

    public static Docker.Literal createLiteral(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        StringWithPadding stringWithPadding = StringWithPadding.of(s);
        String content = stringWithPadding.content();
        Quoting q = Quoting.UNQUOTED;
        if (content.startsWith(DOUBLE_QUOTE) && content.endsWith(DOUBLE_QUOTE)) {
            q = Quoting.DOUBLE_QUOTED;
            content = content.substring(1, content.length() - 1);
        } else if (content.startsWith(SINGLE_QUOTE) && content.endsWith(SINGLE_QUOTE)) {
            q = Quoting.SINGLE_QUOTED;
            content = content.substring(1, content.length() - 1);
        }
        return Docker.Literal.build(
                q, stringWithPadding.prefix(),
                content,
                stringWithPadding.suffix()
        );
    }
}

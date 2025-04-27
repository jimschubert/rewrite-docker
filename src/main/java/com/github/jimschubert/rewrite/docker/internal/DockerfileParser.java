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
import com.github.jimschubert.rewrite.docker.tree.InstructionName;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.jimschubert.rewrite.docker.internal.ParserConstants.*;

/**
 * Parses a Dockerfile into an AST.
 * <p>
 * This parser is not a full implementation of the Dockerfile syntax. It is designed to parse the most common
 * instructions and handle the most common cases. It does not handle all edge cases or all possible syntax.
 */
public class DockerfileParser {
    @SuppressWarnings({"RegExpSimplifiable", "RegExpRedundantEscape"})
    static final Pattern heredocPattern = Pattern.compile("<<[-]?(?<heredoc>[A-Z0-9]{3})([ \\t]*(?<redirect>[>]{0,2})[ \\t]*(?<target>[a-zA-Z0-9_.\\-\\/]*))?");
    private final ParserState state = new ParserState();
    private static final Set<String> instructions = Set.of(
            "ADD", "ARG", "CMD", "COPY", "ENTRYPOINT", "ENV", "EXPOSE", "FROM", "HEALTHCHECK",
            "LABEL", "MAINTAINER", "ONBUILD", "RUN", "SHELL", "STOPSIGNAL", "USER", "VOLUME",
            "WORKDIR", "#"
    );

    private final InstructionParserRegistry registry = new InstructionParserRegistry();
    private final StringBuilder instruction = new StringBuilder();
    private String instructionType = null;

    /**
     * Parses a Dockerfile into an LST.
     *
     * @param input The input stream to parse.
     * @return The parsed Dockerfile as a {@link Docker.Document}.
     */
    public Docker.Document parse(InputStream input) {
        // TODO: handle parser errors, such as unmatched quotes, invalid syntax, etc.
        // TODO: handle syntax version differences (or just support the latest according to https://docs.docker.com/engine/reference/builder/ ??)
        // scan the input stream and maintain state. A newline is the name for a complete instruction unless escaped.
        // when a complete instruction is found, parse it into an AST node
        List<Docker.Stage> stages = new ArrayList<>();
        List<Docker.Instruction> currentInstructions = new ArrayList<>();

        Space eof = Space.EMPTY;
        try (FullLineIterator scanner = new FullLineIterator(input)) {
            while (scanner.hasNext()) {
                String line = scanner.next();
                Space eol = scanner.hasEol() ? Space.build(NEWLINE) : Space.EMPTY;

                // if the line ends in /r, we need to remove it and prepend it to newline above
                if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
                    eol = Space.append(Space.build("\r"), eol);
                    line = line.substring(0, line.length() - 1);
                }

                line = handleLeadingWhitespace(line, state);
                if (line.isEmpty()) {
                    eof = Space.append(eof, eol);
                    continue;
                }

                line = handleRightPadding(line, state);

                // TODO: consider a better way to handle "inline" comments
                if (state.isContinuation() && line.startsWith("#")) {
                    instruction.append(line);
                    instruction.append(eol.getWhitespace());
                    continue;
                }


                String instructionName = peekInstruction(line);
                if (state.isContinuation()
                    && "HEALTHCHECK".equalsIgnoreCase(instructionType)
                    && "CMD".equalsIgnoreCase(instructionName)) {
                    // if we are in a HEALTHCHECK and the next word is CMD, we need to treat this as a continuation
                    // of the previous instruction, not a new one.
                    line = state.prefix().getWhitespace() + line;
                    state.resetPrefix();
                } else if (instructionName != null) {
                    instructionType = instructionName;
                    line = line.substring(instructionName.length());
                } else if (state.prefix() != null && !state.prefix().isEmpty()) {
                    line = state.prefix().getWhitespace() + line;
                    state.resetPrefix();
                }

                String beforeHeredoc = line;
                line = handleHeredoc(line, scanner);
                if (!beforeHeredoc.equals(line)) {
                    eol = Space.EMPTY; // clear, let heredoc handle this.
                }

                instruction.append(line);
                // TODO: should we throw an error here if the line ends in the escape char and there are no more lines?
                if (scanner.hasNext() && line.endsWith(state.getEscapeString())) {
                    instruction.append(eol.getWhitespace());
                    state.isContinuation(true);
                    continue;
                }

                if (!eof.isEmpty() && (state.isContinuation() || scanner.hasNext())) {
                    // any previously gathered whitespace is the prefix to this instruction
                    state.appendPrefix(eof);
                    eof = Space.EMPTY;
                }

                if (!scanner.hasNext()) {
                    // if we are at the end of the file with a newline, that is our eof.
                    // other conditions such as multiple newlines or whitespace are handled earlier
                    eof = Space.append(eof, eol);
                    eol = Space.EMPTY;
                }

                Docker.Instruction instr = registry.getParserFor(instructionType).parse(instruction.toString(), state);
                if (instr != null) {
                    instr = instr.withEol(eol);
                    // if instructionType not upperCase, store the original casing in maker
                    if (!instructionType.equals(instructionType.toUpperCase())) {
                        instr = instr.withMarkers(instr.getMarkers().add(new InstructionName(Tree.randomId(), instructionType)));
                    }
                }
                currentInstructions.add(instr);
                if (instr instanceof Docker.From) {
                    stages.add(new Docker.Stage(Tree.randomId(), new ArrayList<>(currentInstructions), Markers.EMPTY));
                    currentInstructions.clear();
                } else if (!stages.isEmpty()) {
                    // if we have a stage, add the instruction to it
                    stages.get(stages.size() - 1).getChildren().add(instr);
                    currentInstructions.clear();
                }
                reset();
            }
        }

        if (stages.isEmpty()) {
            stages.add(new Docker.Stage(Tree.randomId(), new ArrayList<>(currentInstructions), Markers.EMPTY));
        }

        return new Docker.Document(Tree.randomId(), Paths.get("Dockerfile"), null, null, false, null, stages, eof, Markers.EMPTY);
    }

    /**
     * Reset the parser state. This is used to clear the instruction buffer and reset the parser state.
     */
    private void reset() {
        instruction.setLength(0);
        state.reset();
    }

    /**
     * Handle heredoc syntax in the line. This is used to handle the case where the line contains heredoc syntax.
     * The heredoc syntax is removed from the line and the heredoc content is stored in the parser state.
     *
     * @param line    The line to handle.
     * @param scanner The scanner to use to read the next lines.
     * @return The line without the heredoc syntax.
     */
    private String handleHeredoc(String line, FullLineIterator scanner) {
        // if the line does not have heredoc syntax, return the line
        int heredocIndex = line.indexOf("<<-");
        if (heredocIndex == -1) {
            heredocIndex = line.indexOf("<<");
            if (heredocIndex == -1) {
                return line;
            }
        }

        Matcher matcher = heredocPattern.matcher(line);
        if (!matcher.find()) {
            // not a heredoc
            return line;
        }

        state.heredoc(new Heredoc(line.substring(heredocIndex), matcher.group("heredoc"), matcher.group("target")));

        StringBuilder heredocContent = new StringBuilder(line);
        if (scanner.hasEol()) {
            heredocContent.append(NEWLINE);
        }

        while (scanner.hasNext()) {
            line = scanner.next();
            if (line.trim().equals(state.heredoc().name())) {
                heredocContent.append(line);
                if (scanner.hasEol()) {
                    heredocContent.append(NEWLINE);
                }
                break;
            }

            heredocContent.append(line);
            if (scanner.hasEol()) {
                heredocContent.append(NEWLINE);
            }
        }

        return heredocContent.toString();
    }

    /**
     * Handle leading whitespace of the line. This is used to handle the case where the line starts with whitespace.
     * The whitespace is stored in the parser state and removed from the line.
     *
     * @param line  The line to handle.
     * @param state The parser state.
     * @return The line without the leading whitespace.
     */
    private static String handleLeadingWhitespace(String line, ParserState state) {
        // drain the line of any leading whitespace, storing in parser.addPrefix, then inspect the first "word" to determine the instruction type
        while (line.startsWith(SPACE) || line.startsWith(TAB)) {
            state.appendPrefix(Space.build(line.substring(0, 1)));
            line = line.substring(1);
        }
        return line;
    }

    /**
     * Handle right padding of the line. This is used to handle the case where the line ends with whitespace.
     * The whitespace is stored in the parser state and removed from the line.
     *
     * @param line  The line to handle.
     * @param state The parser state.
     * @return The line without the right padding.
     */
    private static String handleRightPadding(String line, ParserState state) {
        int idx = line.length() - 1;
        // walk line backwards to find the last non-whitespace character
        for (int i = line.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(line.charAt(i))) {
                // move the pointer to after the current non-whitespace character
                idx = i + 1;
                break;
            }
        }

        if (idx < line.length()) {
            state.rightPadding(Space.append(state.rightPadding(), Space.build(line.substring(idx))));
            line = line.substring(0, idx);
        }
        return line;
    }

    /**
     * Peek at the first word of the line to determine if it is a valid instruction.
     *
     * @param line The line to peek at.
     * @return The instruction name, or null if it is not a valid instruction.
     */
    private String peekInstruction(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }

        int start = i;
        if (line.charAt(i) == '#') {
            i++;
        } else {
            while (i < line.length() && Character.isLetter(line.charAt(i))) {
                i++;
            }
        }

        String instr = line.substring(start, i);
        if (instructions.contains(instr.toUpperCase())) {
            return instr;
        }

        return null;
    }

}

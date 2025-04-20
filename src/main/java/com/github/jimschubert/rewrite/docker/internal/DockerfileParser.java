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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.jimschubert.rewrite.docker.internal.ParserConstants.*;
import static com.github.jimschubert.rewrite.docker.internal.ParserUtils.stringToKeyArgs;

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

    // region InstructionParser (private)
    // InstructionParser is used to collect the parts of an instruction and parse it into the appropriate AST node.
    private static class InstructionParser {
        private Quoting quoting = Quoting.UNQUOTED;
        private char escapeChar = 0x5C; // backslash
        private Heredoc heredoc = null;
        private Class<? extends Docker.Instruction> instructionType = null;

        private final ParserState state;

        public InstructionParser(@NonNull ParserState state) {
            this.state = state;
        }

        private final StringBuilder instruction = new StringBuilder();

        void append(String s) {
            instruction.append(s);
        }

        InstructionParser copy() {
            InstructionParser clone = new InstructionParser(state.copy());
            clone.instructionType = this.instructionType;
            clone.quoting = this.quoting;
            clone.escapeChar = this.escapeChar;
            clone.instruction.append(this.instruction);
            return clone;
        }

        void reset() {
            instructionType = null;
            quoting = Quoting.UNQUOTED;
            instruction.setLength(0);
            heredoc = null;
            state.reset();
        }

        private List<DockerRightPadded<Docker.Port>> parsePorts(String input) {
            return parseElements(input, SPACE + TAB, true, ParserUtils::stringToPorts);
        }

        private List<DockerRightPadded<Docker.KeyArgs>> parseArgs(String input) {
            return parseElements(input, SPACE + TAB, true, ParserUtils::stringToKeyArgs);
        }

        private List<DockerRightPadded<Docker.Literal>> parseLiterals(String input) {
            return parseElements(input, SPACE, true, this::createLiteral);
        }

        private List<DockerRightPadded<Docker.Literal>> parseLiterals(Form form, String input) {
            // appendRightPadding is true for shell form, false for exec form
            // exec form is a JSON array, so we need to parse it differently where right padding is after the ']'.
            return parseElements(input, form == Form.EXEC ? COMMA : SPACE, form == Form.SHELL, this::createLiteral);
        }

        private <T> List<DockerRightPadded<T>> parseElements(String input, String delims, boolean appendRightPadding, Function<String, T> elementCreator) {
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
            char escape = this.escapeChar;
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

                        if (inHeredoc && !elements.isEmpty() && elements.get(elements.size() - 1).getElement() instanceof Docker.Literal) {
                            Docker.Literal literal = (Docker.Literal) elements.get(elements.size() - 1).getElement();
                            // allows commands to come after a heredoc. Does not support heredoc within a heredoc or multiple heredocs

                            if (literal.getText() != null && literal.getText().equals(heredoc.name)) {
                                inHeredoc = false;
                            }
                        }

                        // Check if within a heredoc and set escape character to '\n'
                        if (heredoc != null && c == '\n' && !inHeredoc) {
                            inHeredoc = true;
                            afterBuilder.append(c);
                            if (currentElement.length() > 0 && (
                                    currentElement.toString().endsWith(heredoc.indicator()) || (heredoc.redirectionTo != null && currentElement.toString().endsWith(heredoc.redirectionTo)))) {
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

        private Docker.Literal createLiteral(String s) {
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

        Docker.Instruction parse() {
            String name = instructionType.getSimpleName();
            if (name.equals(Docker.Add.class.getSimpleName()) || name.equals(Docker.Copy.class.getSimpleName())) {
                return parseAddLike(name);
            } else if (name.equals(Docker.Arg.class.getSimpleName()) || name.equals(Docker.Label.class.getSimpleName())) {
                return parseArgLike(name);
            } else if (
                    name.equals(Docker.Cmd.class.getSimpleName()) ||
                    name.equals(Docker.Entrypoint.class.getSimpleName()) ||
                    name.equals(Docker.Shell.class.getSimpleName()) ||
                    name.equals(Docker.Volume.class.getSimpleName()) ||
                    name.equals(Docker.Workdir.class.getSimpleName())) {
                return parseCmdLike(name);
            } else if (name.equals(Docker.Comment.class.getSimpleName())) {
                return parseComment();
            } else if (name.equals(Docker.Directive.class.getSimpleName())) {
                return parseDirective();
            } else if (name.equals(Docker.Env.class.getSimpleName()) || name.equals(Docker.StopSignal.class.getSimpleName())) {
                return parseEnvLike(name);
            } else if (name.equals(Docker.Expose.class.getSimpleName())) {
                return refactorExpose();
            } else if (name.equals(Docker.From.class.getSimpleName())) {
                return parseFrom();
            } else if (name.equals(Docker.Healthcheck.class.getSimpleName())) {
                return parseHealthcheck();
            } else if (name.equals(Docker.Maintainer.class.getSimpleName())) {
                return parseMaintainer();
            } else if (name.equals(Docker.OnBuild.class.getSimpleName())) {
                return parseOnBuild();
            } else if (name.equals(Docker.Run.class.getSimpleName())) {
                return parseRun();
            } else if (name.equals(Docker.User.class.getSimpleName())) {
                return parseUser();
            }

            return null;
        }

        private Docker.@NotNull Instruction parseAddLike(String name) {
            // TODO: COPY allows for heredoc with redirection, but ADD does not
            List<DockerRightPadded<Docker.Literal>> literals = parseLiterals(instruction.toString());

            List<Docker.Option> options = new ArrayList<>();
            List<Docker.Literal> sources = new ArrayList<>();
            Docker.Literal destination = null;

            // reverse literals iteration
            for (int i = literals.size() - 1; i >= 0; i--) {
                DockerRightPadded<Docker.Literal> literal = literals.get(i);
                // hack: if we have a heredoc, it'll all become the "sources"
                if (heredoc == null && i == literals.size() - 1) {
                    // the last literal is the destination
                    destination = literal.getElement().withTrailing(Space.append(literal.getElement().getTrailing(), literal.getAfter()));
                    continue;
                }

                if (i == 0 && literal.getElement().getPrefix().isEmpty()) {
                    literal = literal.map(e -> e.withPrefix(state.prefix()));
                }

                String value = literal.getElement().getText();
                if (value.startsWith("--")) {
                    options.add(0, new Docker.Option(
                            Tree.randomId(),
                            literal.getElement().getPrefix(),
                            stringToKeyArgs(literal.getElement().getText()),
                            Markers.EMPTY, literal.getAfter()));
                } else {
                    sources.add(0, literal.getElement().withTrailing(Space.append(literal.getElement().getTrailing(), literal.getAfter())));
                }
            }

            if (name.equals(Docker.Add.class.getSimpleName())) {
                return new Docker.Add(Tree.randomId(), state.prefix(), options,
                        sources,
                        destination,
                        Markers.EMPTY, Space.EMPTY);
            }

            return new Docker.Copy(Tree.randomId(), state.prefix(), options, sources, destination, Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull Instruction parseArgLike(String name) {
            List<DockerRightPadded<Docker.KeyArgs>> args = parseArgs(instruction.toString());

            if (name.equals(Docker.Label.class.getSimpleName())) {
                return new Docker.Label(Tree.randomId(), state.prefix(), args, Markers.EMPTY, Space.EMPTY);
            }
            return new Docker.Arg(Tree.randomId(), state.prefix(), args, Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull Instruction parseCmdLike(String name) {
            String content = instruction.toString();
            Form form = Form.SHELL;
            Space execFormPrefix = Space.EMPTY;
            Space execFormSuffix = Space.EMPTY;
            if (content.trim().startsWith("[")) {
                StringWithPadding stringWithPadding = StringWithPadding.of(content);
                content = stringWithPadding.content();
                execFormPrefix = stringWithPadding.prefix();
                content = content.substring(1, content.length() - 1);
                form = Form.EXEC;

                execFormSuffix = state.rightPadding();
            }

            if (name.equals(Docker.Cmd.class.getSimpleName())) {
                return new Docker.Cmd(Tree.randomId(), form, state.prefix(), execFormPrefix,
                        parseLiterals(form, content).stream()
                                .map(d -> d.getElement().withTrailing(Space.append(d.getElement().getTrailing(), d.getAfter())))
                                .collect(Collectors.toList()), execFormSuffix, Markers.EMPTY, Space.EMPTY);
            } else if (name.equals(Docker.Shell.class.getSimpleName())) {
                return new Docker.Shell(Tree.randomId(), state.prefix(), execFormPrefix, parseLiterals(form, content), execFormSuffix, Markers.EMPTY, Space.EMPTY);
            } else if (name.equals(Docker.Volume.class.getSimpleName())) {
                return new Docker.Volume(Tree.randomId(), form, state.prefix(), execFormPrefix, parseLiterals(form, content), execFormSuffix, Markers.EMPTY, Space.EMPTY);
            } else if (name.equals(Docker.Workdir.class.getSimpleName())) {
                List<DockerRightPadded<Docker.Literal>> lit = parseLiterals(form, content);
                return new Docker.Workdir(Tree.randomId(), state.prefix(), lit.isEmpty() ? null : lit.get(0).getElement(), Markers.EMPTY, Space.EMPTY);
            }

            return new Docker.Entrypoint(Tree.randomId(), form, state.prefix(), execFormPrefix,
                    parseLiterals(form, content).stream()
                            .map(d -> d.getElement().withTrailing(Space.append(d.getElement().getTrailing(), d.getAfter())))
                            .collect(Collectors.toList()), execFormSuffix, Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull Comment parseComment() {
            StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());
            Docker.Literal commentLiteral = createLiteral(stringWithPadding.content()).withPrefix(stringWithPadding.prefix());
            return new Docker.Comment(
                    Tree.randomId(),
                    state.prefix(),
                    commentLiteral.withTrailing(Space.append(commentLiteral.getTrailing(), state.rightPadding())),
                    Markers.EMPTY,
                    Space.EMPTY
            );
        }

        private Docker.@NotNull Directive parseDirective() {
            StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());

            String[] parts = stringWithPadding.content().split("=", 2);
            String key = parts.length > 0 ? parts[0] : "";
            String value = parts.length > 1 ? parts[1] : "";

            if (key.equalsIgnoreCase("escape")) {
                escapeChar = value.charAt(0);
            }

            return new Docker.Directive(Tree.randomId(), state.prefix(),
                    new DockerRightPadded<>(
                            new Docker.KeyArgs(stringWithPadding.prefix(), Docker.Literal.build(key), Docker.Literal.build(value), true, quoting),
                            state.rightPadding(),
                            Markers.EMPTY
                    ),
                    Markers.EMPTY,
                    Space.EMPTY);
        }

        private Docker.@NotNull Instruction parseEnvLike(String name) {
            List<DockerRightPadded<Docker.KeyArgs>> args = parseArgs(instruction.toString());
            if (name.equals(Docker.Env.class.getSimpleName())) {
                return new Docker.Env(Tree.randomId(), state.prefix(), args, Markers.EMPTY, Space.EMPTY);
            }

            Docker.Literal signal = null;
            if (!args.isEmpty()) {
                DockerRightPadded<Docker.KeyArgs> arg = args.get(0);
                signal = Docker.Literal.build(Quoting.UNQUOTED, arg.getElement().getPrefix(), arg.getElement().key(), arg.getAfter());
            }

            return new Docker.StopSignal(Tree.randomId(), state.prefix(), signal, Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull Expose refactorExpose() {
            List<DockerRightPadded<Docker.Port>> ports = parsePorts(instruction.toString());

            return new Docker.Expose(Tree.randomId(), state.prefix(), ports, Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull From parseFrom() {
            String content = instruction.toString();
            Docker.Literal platform = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
            Docker.Literal image = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
            Docker.Literal version = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
            Docker.Literal as = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);
            Docker.Literal alias = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, null, Space.EMPTY);

            List<DockerRightPadded<Docker.Literal>> literals = parseLiterals(content);
            if (!literals.isEmpty()) {
                while (!literals.isEmpty()) {
                    DockerRightPadded<Docker.Literal> literal = literals.remove(0);
                    Docker.Literal elem = literal.getElement();
                    String value = literal.getElement().getText();
                    Space after = Space.append(elem.getTrailing(), literal.getAfter());
                    if (value.startsWith("--platform")) {
                        platform = elem.withTrailing(after);
                    } else if (image.getText() != null && "as".equalsIgnoreCase(value)) {
                        as = elem.withTrailing(after);
                    } else if ("as".equalsIgnoreCase(as.getText())) {
                        alias = elem.withTrailing(after);
                    } else if (image.getText() == null) {
                        image = elem.withTrailing(after);
                        String imageText = literal.getElement().getText();
                        // walk imageText forwards to find the first ':' or '@' to determine the version
                        int idx = 0;
                        for (char c : imageText.toCharArray()) {
                            if (c == ':' || c == '@') {
                                break;
                            }
                            idx++;
                        }

                        if (idx < imageText.length() - 1) {
                            version = createLiteral(imageText.substring(idx))
                                    .withPrefix(Space.EMPTY)
                                    .withTrailing(image.getTrailing());
                            imageText = imageText.substring(0, idx);

                            String img = imageText;
                            image = image.withText(img).withTrailing(Space.EMPTY);
                        }
                    }
                }
            }

            return new Docker.From(Tree.randomId(), state.prefix(), platform, image, version, as, alias, state.rightPadding(), Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull Healthcheck parseHealthcheck() {
            StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());
            String content = stringWithPadding.content();
            List<Docker.Literal> commands;
            if (content.equalsIgnoreCase("NONE")) {
                commands = new ArrayList<>();
                Docker.Literal none = Docker.Literal.build(Quoting.UNQUOTED, stringWithPadding.prefix(), content, stringWithPadding.suffix());
                commands.add(none.withTrailing(state.rightPadding()));
                return new Docker.Healthcheck(Tree.randomId(), state.prefix(), Docker.Healthcheck.Type.NONE, null, commands, Markers.EMPTY, Space.EMPTY);
            }

            List<DockerRightPadded<Docker.KeyArgs>> args;
            String[] parts = instruction.toString().split("CMD", 2);
            if (parts.length > 1) {
                // the first part is the options, but keyargs don't support trailing spaces
                StringWithPadding swp = StringWithPadding.of(parts[0]);
                // HACK if swp is all whitespace, we'll ignore it for now
                if (!swp.content().isEmpty()) {
                    args = parseArgs(swp.prefix().getWhitespace() + swp.content());
                } else {
                    args = new ArrayList<>();
                }

                // the second part is the command, prefix it with any keyargs trailing whitespace
                commands = parseLiterals(Form.SHELL, swp.suffix().getWhitespace() + "CMD" + parts[1])
                        .stream().map(d ->
                                d.getElement()
                                .withTrailing(Space.append(d.getElement().getTrailing(), d.getAfter())))
                        .collect(Collectors.toList());
            } else {
                args = new ArrayList<>();
                commands = parseLiterals(Form.SHELL, content)
                        .stream().map(d ->
                                d.getElement().withTrailing(Space.append(d.getElement().getTrailing(), d.getAfter())))
                        .collect(Collectors.toList());
            }

            return new Docker.Healthcheck(Tree.randomId(), state.prefix(), Docker.Healthcheck.Type.CMD, args, commands, Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull Maintainer parseMaintainer() {
            StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());
            return new Docker.Maintainer(Tree.randomId(), quoting, state.prefix(),
                    Docker.Literal.build(stringWithPadding.content())
                            .withPrefix(stringWithPadding.prefix())
                            .withTrailing(Space.append(stringWithPadding.suffix(), state.rightPadding())),
                    Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull OnBuild parseOnBuild() {
            StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());
            state.prefix(Space.append(state.prefix(), stringWithPadding.prefix()));

            String content = stringWithPadding.content();
            if (stringWithPadding.suffix() != null) {
                state.rightPadding(Space.append(stringWithPadding.suffix(), state.rightPadding()));
            }

            InstructionParser nestedInstruction = this.copy();
            content = handleInstructionType(content, nestedInstruction, state);
            nestedInstruction.instruction.append(content);
            Docker nested = nestedInstruction.parse();

            return new Docker.OnBuild(Tree.randomId(), state.prefix(), nested, state.rightPadding(), Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull Run parseRun() {
            // TODO: Run allows for JSON array syntax (exec form)
            List<DockerRightPadded<Docker.Literal>> literals = parseLiterals(instruction.toString());
            List<Docker.Option> options = new ArrayList<>();
            List<Docker.Literal> commands = new ArrayList<>();

            boolean doneWithOptions = false;
            for (DockerRightPadded<Docker.Literal> literal : literals) {
                String value = literal.getElement().getText();
                if (!doneWithOptions && (value.startsWith("--mount") || value.startsWith("--network") || value.startsWith("--security"))) {
                    options.add(new Docker.Option(
                            Tree.randomId(),
                            literal.getElement().getPrefix(),
                            stringToKeyArgs(literal.getElement().getText()),
                            Markers.EMPTY, literal.getAfter()));
                } else {
                    doneWithOptions = true;
                    commands.add(literal.getElement().withTrailing(Space.append(literal.getElement().getTrailing(), literal.getAfter())));
                }
            }

            return new Docker.Run(Tree.randomId(), state.prefix(), options, commands, Markers.EMPTY, Space.EMPTY);
        }

        private Docker.@NotNull User parseUser() {
            StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());
            String[] parts = stringWithPadding.content().split(":", 2);

            Docker.Literal user;
            Docker.Literal group = null;
            if (parts.length > 1) {
                user = Docker.Literal.build(Quoting.UNQUOTED, stringWithPadding.prefix(), parts[0], Space.EMPTY);
                group = Docker.Literal.build(Quoting.UNQUOTED, Space.EMPTY, parts[1], Space.append(stringWithPadding.suffix(), state.rightPadding()));
            } else {
                user = Docker.Literal.build(Quoting.UNQUOTED, stringWithPadding.prefix(), parts[0], Space.append(stringWithPadding.suffix(), state.rightPadding()));
            }

            return new Docker.User(Tree.randomId(), state.prefix(), user, group, Markers.EMPTY, Space.EMPTY);
        }
    }
    // endregion

    /**
     * Parses a Dockerfile into an AST.
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
        InstructionParser parser = new InstructionParser(state);

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

                if (scanner.hasNext() && !eof.isEmpty()) {
                    // at this point, we have a line, so eof is the line prefix
                    state.appendPrefix(eof);
                    eof = Space.EMPTY;
                } else if (!scanner.hasNext()) {
                    // if we are at the end of the file with a newline
                    eof = Space.append(eof, eol);
                    eol = Space.EMPTY;
                }

                line = handleRightPadding(line, state);

                // TODO: consider a better way to handle "inline" comments
                if (state.isContinuation() && line.startsWith("#")) {
                    parser.append(line);
                    parser.append(eol.getWhitespace());
                    continue;
                }

                line = handleInstructionType(line, parser, state);
                String beforeHeredoc = line;
                line = handleHeredoc(line, parser, scanner);
                if (!beforeHeredoc.equals(line)) {
                    eol = Space.EMPTY;// clear, let heredoc handle this.
                }

                if (parser.instructionType == Docker.Comment.class) {
                    handleDirectiveSpecialCase(line, parser);
                }

                parser.append(line);
                if (line.endsWith(state.getEscapeString())) {
                    parser.append(eol.getWhitespace());
                    state.isContinuation(true);
                    continue;
                }

                Docker.Instruction instr = parser.parse();
                if (instr != null) {
                    instr = instr.withEol(eol);
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
                parser.reset();
            }
        }

        if (stages.isEmpty()) {
            stages.add(new Docker.Stage(Tree.randomId(), new ArrayList<>(currentInstructions), Markers.EMPTY));
        }

        return new Docker.Document(Tree.randomId(), Paths.get("Dockerfile"), null, null, false, null, stages, eof, Markers.EMPTY);
    }

    private String handleHeredoc(String line, InstructionParser parser, FullLineIterator scanner) {
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

        parser.heredoc = new Heredoc(line.substring(heredocIndex), matcher.group("heredoc"), matcher.group("target"));

        StringBuilder heredocContent = new StringBuilder(line);
        if (scanner.hasEol()) {
            heredocContent.append(NEWLINE);
        }
        while (scanner.hasNext()) {
            line = scanner.next();
            if (line.trim().equals(parser.heredoc.name)) {
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

    private static String handleLeadingWhitespace(String line, ParserState state) {
        // drain the line of any leading whitespace, storing in parser.addPrefix, then inspect the first "word" to determine the instruction type
        while (line.startsWith(SPACE) || line.startsWith(TAB)) {
            state.appendPrefix(Space.build(line.substring(0, 1)));
            line = line.substring(1);
        }
        return line;
    }

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

    private static void handleDirectiveSpecialCase(String line, InstructionParser parser) {
        String lower = line.toLowerCase().trim();
        // hack: if comment is a directive, change the type accordingly
        // directives are used so rarely that I don't care to make this much more robust atm
        if ((lower.startsWith("syntax=") || lower.startsWith("escape=") || lower.startsWith("check=")) && !lower.contains(" ")) {
            parser.instructionType = Docker.Directive.class;
        }
    }

    private static String handleInstructionType(String line, InstructionParser parser, ParserState state) {
        // take line until the first space character
        int spaceIndex = line.indexOf(' ');
        String firstWord = (spaceIndex == -1) ? line : line.substring(0, spaceIndex);
        Class<? extends Docker.Instruction> instructionType = instructionFromText(firstWord);
        if (state.isContinuation() && Docker.Healthcheck.class == parser.instructionType && instructionType == Docker.Cmd.class) {
            // special case for healthcheck in which CMD can exist on a continuation line
            //noinspection DataFlowIssue
            parser.instructionType = Docker.Healthcheck.class;
            // if there was any prefix stored previously, add it to the multiline instruction
            line = state.prefix().getWhitespace() + line;
            state.resetPrefix();
        } else if (instructionType != null) {
            parser.instructionType = instructionType;
            // remove the first word from line
            line = (spaceIndex == -1) ? line : line.substring(spaceIndex);
        } else if (state.prefix() != null && !state.prefix().isEmpty()) {
            // if there was any prefix stored previously, add it to the multiline instruction
            // this is a special case for multi-line instructions like comments
            line = state.prefix().getWhitespace() + line;
            state.resetPrefix();
        }
        return line;
    }

    private static Class<? extends Docker.Instruction> instructionFromText(String s) {
        switch (s.toUpperCase()) {
            case ADD:
                return Docker.Add.class;
            case ARG:
                return Docker.Arg.class;
            case CMD:
                return Docker.Cmd.class;
            case COPY:
                return Docker.Copy.class;
            case ENTRYPOINT:
                return Docker.Entrypoint.class;
            case ENV:
                return Docker.Env.class;
            case EXPOSE:
                return Docker.Expose.class;
            case FROM:
                return Docker.From.class;
            case HEALTHCHECK:
                return Docker.Healthcheck.class;
            case LABEL:
                return Docker.Label.class;
            case MAINTAINER:
                return Docker.Maintainer.class;
            case ONBUILD:
                return Docker.OnBuild.class;
            case RUN:
                return Docker.Run.class;
            case SHELL:
                return Docker.Shell.class;
            case STOPSIGNAL:
                return Docker.StopSignal.class;
            case USER:
                return Docker.User.class;
            case VOLUME:
                return Docker.Volume.class;
            case WORKDIR:
                return Docker.Workdir.class;
            case COMMENT:
                return Docker.Comment.class;
            default:
                return null;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @NoArgsConstructor(force = true)
    @Accessors(fluent = true)
    private static class Heredoc {
        String indicator;
        String name;
        String redirectionTo;
    }
}

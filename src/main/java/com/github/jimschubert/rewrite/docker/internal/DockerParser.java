package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.*;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class DockerParser {
    static final String DOUBLE_QUOTE = "\"";
    static final String SINGLE_QUOTE = "'";
    static final String BACKSLASH = "\\";
    static final String TAB = "\t";
    static final String NEWLINE = "\n";
    static final String CARRIAGE_RETURN = "\r";
    static final String EQUAL = "=";
    static final String SPACE = " ";
    static final String EMPTY = "";
    static final String COMMA = ",";

    // InstructionParser is used to collect the parts of an instruction and parse it into the appropriate AST node.
    static class InstructionParser {
        private Space prefix = Space.EMPTY;
        private Space rightPadding = Space.EMPTY;
        private Quoting quoting = Quoting.UNQUOTED;
        private char escapeChar = 0x5C; // backslash
        private Class<? extends Docker.Instruction> instructionType = null;

        private final StringBuilder instruction = new StringBuilder();

        String getEscapeChar() {
            return String.valueOf(escapeChar);
        }

        void appendPrefix(Space prefix) {
            if (prefix != null) {
                this.prefix = prefix.withWhitespace(this.prefix.getWhitespace() + prefix.getWhitespace());
            }
        }

        void resetPrefix() {
            prefix = Space.EMPTY;
        }

        void append(String s) {
            instruction.append(s);
        }

        void reset(){
            instructionType = null;
            prefix = Space.EMPTY;
            rightPadding = Space.EMPTY;
            quoting = Quoting.UNQUOTED;
            instruction.setLength(0);
        }

        Docker.Port stringToPorts(String s) {
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

        Docker.KeyArgs stringToKeyArgs(String s) {
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
            return new Docker.KeyArgs(stringWithPadding.prefix(), key, value, EQUAL.equals(delim), q);
        }

        private List<DockerRightPadded<Docker.Port>> parsePorts(String input) {
            return parseElements(input, SPACE + TAB, true, this::stringToPorts);
        }

        private List<DockerRightPadded<Docker.KeyArgs>> parseArgs(String input) {
            return parseElements(input, SPACE + TAB, true, this::stringToKeyArgs);
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
            boolean inQuotes = false;
            char doubleQuote = DOUBLE_QUOTE.charAt(0);
            char singleQuote = SINGLE_QUOTE.charAt(0);
            char escape = this.escapeChar;
            char quote = 0;
            char lastChar = 0;
            char comma = ',';

            // create a lookup of chars from delims
            HashSet<Character> delimSet = new HashSet<>();
            for (char c : delims.toCharArray()) {
                delimSet.add(c);
            }

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (inQuotes) {
                    if (c == quote && lastChar != escape) {
                        inQuotes = false;
                    }
                    currentElement.append(c);
                } else {
                    if (delimSet.contains(c) && lastChar != escape) {
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
                        if (c == doubleQuote || c == singleQuote) {
                            inQuotes = true;
                            quote = c;
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
                        if (!afterBuilder.isEmpty() && (c == ' ' || c == '\t' || c == '\n')) {
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
                        if (!elements.isEmpty() && !afterBuilder.isEmpty()) {
                            int idx = elements.size() - 1;
                            DockerRightPadded<T> element = elements.get(idx);
                            elements.set(idx, element.withAfter( Space.append(element.getAfter(), Space.build(afterBuilder.toString()))));
                            afterBuilder.setLength(0);
                        }

                        // Only collect the current element if we're not "in a prefix" situation
                        if (afterBuilder.isEmpty()) {
                            currentElement.append(c);
                        }
                    }
                }
                lastChar = c;
            }

            if (!currentElement.isEmpty()) {
                // if it's whitespace only, add it as "after" to the last element
                if (StringUtils.isBlank(currentElement.toString())) {
                    if (!elements.isEmpty()) {
                        int idx = elements.size() - 1;
                        elements.set(idx, elements.get(idx).withAfter(Space.build(currentElement.toString())));
                    }
                } else {
                    DockerRightPadded<T> element = DockerRightPadded.build(elementCreator.apply(currentElement.toString()));
                    if (appendRightPadding) {
                        element = element.withAfter(rightPadding);
                    }
                    elements.add(element);
                }
            }

            if (!afterBuilder.isEmpty()) {
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
                    stringWithPadding.prefix(),
                    content,
                    stringWithPadding.suffix(),
                    q);
        }

        Docker.Instruction parse() {
            String name = instructionType.getSimpleName();
            if (name.equals(Docker.Add.class.getSimpleName()) || name.equals(Docker.Copy.class.getSimpleName())) {
                List<DockerRightPadded<Docker.Literal>> literals = parseLiterals(instruction.toString());

                List<DockerRightPadded<Docker.Option>> options = new ArrayList<>();
                List<DockerRightPadded<Docker.Literal>> sources = new ArrayList<>();
                DockerRightPadded<Docker.Literal> destination = null;

                // reverse literals iteration
                for (int i = literals.size() - 1; i >= 0; i--) {
                    DockerRightPadded<Docker.Literal> literal = literals.get(i);
                    if (i == literals.size() - 1) {
                        // the last literal is the destination
                        destination = literal;
                        continue;
                    }

                    String value = literal.getElement().getText();
                    if (value.startsWith("--")) {
                        options.add(0, DockerRightPadded.build(new Docker.Option(
                                Tree.randomId(),
                                literal.getElement().getPrefix(),
                                stringToKeyArgs(literal.getElement().getText()),
                                Markers.EMPTY)).withAfter(literal.getAfter()));
                    } else {
                        sources.add(0, literal);
                    }
                }

                if (name.equals(Docker.Add.class.getSimpleName())) {
                    return new Docker.Add(Tree.randomId(), prefix, options, sources, destination, Markers.EMPTY);
                }

                return new Docker.Copy(Tree.randomId(), prefix, options, sources, destination, Markers.EMPTY);
            } else if (name.equals(Docker.Arg.class.getSimpleName()) || name.equals(Docker.Label.class.getSimpleName())) {

                List<DockerRightPadded<Docker.KeyArgs>> args = parseArgs(instruction.toString());

                if (name.equals(Docker.Label.class.getSimpleName())) {
                    return new Docker.Label(Tree.randomId(), prefix, Markers.EMPTY, args);
                }
                return new Docker.Arg(Tree.randomId(), prefix, Markers.EMPTY, args);
            } else if (
                    name.equals(Docker.Cmd.class.getSimpleName()) ||
                    name.equals(Docker.Entrypoint.class.getSimpleName()) ||
                    name.equals(Docker.Shell.class.getSimpleName()) ||
                    name.equals(Docker.Volume.class.getSimpleName()) ||
                    name.equals(Docker.Workdir.class.getSimpleName())){
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

                    execFormSuffix = rightPadding;
                }

                if (name.equals(Docker.Cmd.class.getSimpleName())) {
                    return new Docker.Cmd(Tree.randomId(), form, prefix, execFormPrefix, parseLiterals(form, content), execFormSuffix, Markers.EMPTY);
                } else if (name.equals(Docker.Shell.class.getSimpleName())) {
                    return new Docker.Shell(Tree.randomId(), prefix, execFormPrefix, parseLiterals(form, content), execFormSuffix, Markers.EMPTY);
                } else if (name.equals(Docker.Volume.class.getSimpleName())) {
                    return new Docker.Volume(Tree.randomId(), form, prefix, execFormPrefix, parseLiterals(form, content), execFormSuffix, Markers.EMPTY);
                } else if (name.equals(Docker.Workdir.class.getSimpleName())) {
                    List<DockerRightPadded<Docker.Literal>> lit = parseLiterals(form, content);
                    return new Docker.Workdir(Tree.randomId(), prefix, lit.isEmpty()? null: lit.get(0).getElement(), Markers.EMPTY);
                }

                return new Docker.Entrypoint(Tree.randomId(), form, prefix, execFormPrefix, parseLiterals(form, content), execFormSuffix, Markers.EMPTY);
            } else if (name.equals(Docker.Comment.class.getSimpleName())) {
                StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());

                return new Docker.Comment(Tree.randomId(), prefix, Markers.EMPTY,
                        DockerRightPadded.build(createLiteral(stringWithPadding.content()).withPrefix(stringWithPadding.prefix())).withAfter(rightPadding));
            } else if (name.equals(Docker.Directive.class.getSimpleName())) {
                StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());

                String[] parts = stringWithPadding.content().split("=", 2);
                String key = parts.length > 0 ? parts[0] : "";
                String value = parts.length > 1 ? parts[1] : "";

                if ( key.equalsIgnoreCase("escape")) {
                    escapeChar = value.charAt(0);
                }

                return new Docker.Directive(Tree.randomId(), prefix, Markers.EMPTY, new DockerRightPadded<>(
                        new Docker.KeyArgs(stringWithPadding.prefix(), key, value, true, quoting),
                        rightPadding,
                        Markers.EMPTY
                ));
            } else if (name.equals(Docker.Env.class.getSimpleName()) || name.equals(Docker.StopSignal.class.getSimpleName())) {
                List<DockerRightPadded<Docker.KeyArgs>> args = parseArgs(instruction.toString());
                if (name.equals(Docker.Env.class.getSimpleName())) {
                    return new Docker.Env(Tree.randomId(), prefix, Markers.EMPTY, args);
                }

                Docker.Literal signal = null;
                if (!args.isEmpty()) {
                    DockerRightPadded<Docker.KeyArgs> arg = args.get(0);
                    signal = Docker.Literal.build(arg.getElement().getPrefix(), arg.getElement().getKey(), arg.getAfter(), Quoting.UNQUOTED);
                }

                return new Docker.StopSignal(Tree.randomId(), prefix, Markers.EMPTY, signal);
            }  else if (name.equals(Docker.Expose.class.getSimpleName())) {
                List<DockerRightPadded<Docker.Port>> ports = parsePorts(instruction.toString());

                return new Docker.Expose(Tree.randomId(), prefix, Markers.EMPTY, ports);
            } else if (name.equals(Docker.From.class.getSimpleName())) {
                String content = instruction.toString();
                DockerRightPadded<Docker.Literal> platform = null;
                DockerRightPadded<Docker.Literal> image = null;
                DockerRightPadded<Docker.Literal> version = null;
                DockerRightPadded<Docker.Literal> as = null;
                DockerRightPadded<Docker.Literal> alias = null;

                List<DockerRightPadded<Docker.Literal>> literals = parseLiterals(content);
                if (!literals.isEmpty()) {
                    while (!literals.isEmpty()) {
                        DockerRightPadded<Docker.Literal> literal = literals.remove(0);
                        String value = literal.getElement().getText();
                        if (value.startsWith("--platform")) {
                            platform = literal;
                            // remove "--platform=" from the value
                            value = value.substring(value.indexOf('=') + 1);
                            platform = platform.withElement(platform.getElement().withText(value));
                        } else if (value.equalsIgnoreCase("as")) {
                            as = literal;
                        } else if (image != null && as != null) {
                            alias = literal;
                        } else if (image == null) {
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
                                image = literal;
                                version = DockerRightPadded.build(createLiteral(imageText.substring(idx))).withAfter(image.getAfter());
                                imageText = imageText.substring(0, idx);
                                image = image.withAfter(Space.EMPTY).withElement(image.getElement().withText(imageText));
                            }
                        }
                    }
                }

                return new Docker.From(Tree.randomId(), prefix, Markers.EMPTY, platform, image, version, as == null ? null : as.getElement(), alias, rightPadding);
            } else if (name.equals(Docker.Healthcheck.class.getSimpleName())) {
                StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());
                String content = stringWithPadding.content();
                List<DockerRightPadded<Docker.Literal>> commands;
                if (content.equalsIgnoreCase("NONE")) {
                    commands = new ArrayList<>();
                    Docker.Literal none = Docker.Literal.build(stringWithPadding.prefix(), content, stringWithPadding.suffix(), Quoting.UNQUOTED);
                    commands.add(DockerRightPadded.build(none).withAfter(rightPadding));
                    return new Docker.Healthcheck(Tree.randomId(), prefix, Docker.Healthcheck.Type.NONE, null, commands, Markers.EMPTY);
                }

                List<DockerRightPadded<Docker.KeyArgs>> args;
                String[] parts = instruction.toString().split("CMD", 2);
                if (parts.length > 1) {
                    // the first part is the options, but keyargs don't support trailing spaces
                    StringWithPadding swp = StringWithPadding.of(parts[0]);
                    args = parseArgs(swp.prefix().getWhitespace() + swp.content());
                    // the second part is the command, prefix it with any keyargs trailing whitespace
                    commands = parseLiterals(Form.SHELL, swp.suffix().getWhitespace() + "CMD" + parts[1]);
                } else {
                    args = new ArrayList<>();
                    commands = parseLiterals(Form.SHELL, content);
                }

                return new Docker.Healthcheck(Tree.randomId(), stringWithPadding.prefix(), Docker.Healthcheck.Type.CMD, args, commands, Markers.EMPTY);
            } else if (name.equals(Docker.Maintainer.class.getSimpleName())) {
                return new Docker.Maintainer (Tree.randomId(), prefix, Markers.EMPTY, instruction.toString(), quoting);
            } else if (name.equals(Docker.OnBuild.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.OnBuild(Tree.randomId(), prefix, Markers.EMPTY, null);
            }  else if (name.equals(Docker.Run.class.getSimpleName())) {
                List<String> commands = new ArrayList<>();
                if (instruction.toString().contains(escapeChar + NEWLINE)) {
                    Space indent = Space.EMPTY;
                    String[] parts = instruction.toString().split(escapeChar + "\\n");
                    if (parts.length > 1) {
                        // collect all leading whitespace of the first part
                        int idx = 0;
                        for (char c : parts[0].toCharArray()) {
                            if (c == ' ' || c == '\t') {
                                idx++;
                                indent = indent.withWhitespace(indent.getWhitespace() + c);
                            } else {
                                break;
                            }
                        }
                        commands.add(parts[0].substring(idx));
                    }
                } else {
                    commands.add(instruction.toString());
                }

                // TODO: implement this Options should be a list of KeyArgs, Commands should be a list of RightPadded literals
                return new Docker.Run(Tree.randomId(), prefix, Markers.EMPTY, null, null, null, null, null, null);
            } else if (name.equals(Docker.User.class.getSimpleName())) {
                StringWithPadding stringWithPadding = StringWithPadding.of(instruction.toString());
                String[] parts = stringWithPadding.content().split(":", 2);

                Docker.Literal user;
                Docker.Literal group = null;
                if (parts.length > 1) {
                    user = Docker.Literal.build(stringWithPadding.prefix(), parts[0], Space.EMPTY, Quoting.UNQUOTED);
                    group = Docker.Literal.build(Space.EMPTY, parts[1], Space.append(stringWithPadding.suffix(), rightPadding), Quoting.UNQUOTED);
                } else {
                    user = Docker.Literal.build(stringWithPadding.prefix(), parts[0], Space.append(stringWithPadding.suffix(), rightPadding), Quoting.UNQUOTED);
                }

                return new Docker.User(Tree.randomId(), prefix, Markers.EMPTY, user, group);
            }

            return null;
        }
    }

    public Docker parse(InputStream input) {
        // scan the input stream and maintain state. A newline is the delimiter for a complete instruction unless escaped.
        // when a complete instruction is found, parse it into an AST node
        List<Docker.Stage> stages = new ArrayList<>();
        List<Docker.Instruction> currentInstructions = new ArrayList<>();

        Space eof = Space.EMPTY;
        InstructionParser parser = new InstructionParser();

        try (Scanner scanner = new Scanner(input)) {
            boolean lastLineContinued = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) {
                    // tracks newlines for continuations and prefixes
                    parser.appendPrefix(Space.build(NEWLINE));
                    continue;
                }

                line = handleLeadingWhitespace(line, parser);
                if (line.isEmpty()) {
                    continue;
                }

                line = handleRightPadding(line, parser);
                line = handleInstructionType(line, parser, lastLineContinued);

                if (parser.instructionType == Docker.Comment.class) {
                    handleDirectiveSpecialCase(line, parser);
                }

                parser.append(line);
                if (line.endsWith(parser.getEscapeChar())) {
                    parser.append(NEWLINE);
                    lastLineContinued = true;
                    continue;
                }

                Docker.Instruction instr = parser.parse();
                currentInstructions.add(instr);
                if (instr instanceof Docker.From) {
                    stages.add(new Docker.Stage(Tree.randomId(), Markers.EMPTY, new ArrayList<>(currentInstructions)));
                    currentInstructions.clear();
                }

                parser.reset();
                lastLineContinued = false;
            }
        }

        if (stages.isEmpty()) {
            stages.add(new Docker.Stage(Tree.randomId(), Markers.EMPTY, new ArrayList<>(currentInstructions)));
        }

        return new Docker.Document(Tree.randomId(), Markers.EMPTY, Paths.get("Dockerfile"), null, null, false, null, stages, eof);
    }


    private String handleLeadingWhitespace(String line, InstructionParser parser) {
        // drain the line of any leading whitespace, storing in parser.addPrefix, then inspect the first "word" to determine the instruction type
        while (line.startsWith(SPACE) || line.startsWith(TAB)) {
            parser.appendPrefix(Space.build(line.substring(0, 1)));
            line = line.substring(1);
        }
        return line;
    }

    private String handleInstructionType(String line, InstructionParser parser, boolean isContinuation) {
        // take line until the first space character
        int spaceIndex = line.indexOf(' ');
        String firstWord = (spaceIndex == -1) ? line : line.substring(0, spaceIndex);
        Class<? extends Docker.Instruction> instructionType = instructionFromText(firstWord);
        if (isContinuation && Docker.Healthcheck.class == parser.instructionType && instructionType == Docker.Cmd.class) {
            // special case for healthcheck in which CMD can exist on a continuation line
            //noinspection DataFlowIssue
            parser.instructionType = Docker.Healthcheck.class;
            // if there was any prefix stored previously, add it to the multiline instruction
            line = parser.prefix.getWhitespace() + line;
            parser.resetPrefix();
        } else if (instructionType != null) {
            parser.instructionType = instructionType;
            // remove the first word from line
            line = (spaceIndex == -1) ? line : line.substring(spaceIndex);
        } else if (parser.prefix != null && !parser.prefix.isEmpty()) {
            // if there was any prefix stored previously, add it to the multiline instruction
            // this is a special case for multi-line instructions like comments
            line = parser.prefix.getWhitespace() + line;
            parser.resetPrefix();
        }
        return line;
    }

    private String handleRightPadding(String line, InstructionParser parser) {
        int idx = line.length() - 1;
        // walk line backwards to find the last non-whitespace character
        for (int i = line.length() - 1; i >= 0; i--) {
            if (line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                // move the pointer to after the current non-whitespace character
                idx = i + 1;
                break;
            }
        }

        if (idx < line.length()) {
            parser.rightPadding = Space.build(line.substring(idx));
            line = line.substring(0, idx);
        }
        return line;
    }

    private void handleDirectiveSpecialCase(String line, InstructionParser parser) {
        String lower = line.toLowerCase().trim();
        // hack: if comment is a directive, change the type accordingly
        // directives are used so rarely that I don't care to make this much more robust atm
        if ((lower.startsWith("syntax=") || lower.startsWith("escape=") || lower.startsWith("check=")) && !lower.contains(" ")) {
            parser.instructionType = Docker.Directive.class;
        }
    }

    private Class<? extends Docker.Instruction> instructionFromText(String s) {
        return switch (s.toUpperCase()) {
            case "ADD" -> Docker.Add.class;
            case "ARG" -> Docker.Arg.class;
            case "CMD" -> Docker.Cmd.class;
            case "COPY" -> Docker.Copy.class;
            case "ENTRYPOINT" -> Docker.Entrypoint.class;
            case "ENV" -> Docker.Env.class;
            case "EXPOSE" -> Docker.Expose.class;
            case "FROM" -> Docker.From.class;
            case "HEALTHCHECK" -> Docker.Healthcheck.class;
            case "LABEL" -> Docker.Label.class;
            case "MAINTAINER" -> Docker.Maintainer.class;
            case "ONBUILD" -> Docker.OnBuild.class;
            case "RUN" -> Docker.Run.class;
            case "SHELL" -> Docker.Shell.class;
            case "STOPSIGNAL" -> Docker.StopSignal.class;
            case "USER" -> Docker.User.class;
            case "VOLUME" -> Docker.Volume.class;
            case "WORKDIR" -> Docker.Workdir.class;
            case "#" -> Docker.Comment.class;
            default -> null;
        };
    }
}

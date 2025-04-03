package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.*;
import org.jetbrains.annotations.NotNull;
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

        Docker.KeyArgs stringToKeyArgs(String s) {
            if (s == null || s.isEmpty()) {
                return null;
            }

            StringWithPadding stringWithPadding = asStringWithPrefix(s);
            String content = stringWithPadding.content();

            @SuppressWarnings("RegExpRepeatedSpace")
            String delim = content.contains(EQUAL) ? EQUAL : SPACE;
            String[] parts = content.split(delim, EQUAL.equals(delim) ? 2 : 0);
            String key = parts.length > 0 ? parts[0] : EMPTY;
            String value = parts.length > 1 ? parts[1].trim() : null;
            Quoting q = Quoting.UNQUOTED;

            if (value != null) {
                if (value.startsWith("\"") && value.endsWith(DOUBLE_QUOTE)) {
                    q = Quoting.DOUBLE_QUOTED;
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith(SINGLE_QUOTE)) {
                    q = Quoting.SINGLE_QUOTED;
                    value = value.substring(1, value.length() - 1);
                }
            }
            return new Docker.KeyArgs(stringWithPadding.prefix(), key, value, EQUAL.equals(delim), q);
        }

        private List<DockerRightPadded<Docker.KeyArgs>> parseArgs(String input) {
            return parseElements(input, " \t", true, this::stringToKeyArgs);
        }

        private List<DockerRightPadded<Docker.Literal>> parseLiterals(Form form, String input) {
            // appendRightPadding is true for shell form, false for exec form
            // exec form is a JSON array, so we need to parse it differently where right padding is after the ']'.
            return parseElements(input, form == Form.EXEC ? "," : " ", form == Form.SHELL, this::createLiteral);
        }

        private <T> List<DockerRightPadded<T>> parseElements(String input, String delims, boolean appendRightPadding, Function<String, T> elementCreator) {
            List<DockerRightPadded<T>> elements = new ArrayList<>();
            StringBuilder currentElement = new StringBuilder();
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
                            elements.add(DockerRightPadded.build(elementCreator.apply(currentElement.toString())).withAfter(Space.EMPTY));
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
                        currentElement.append(c);
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
                    return elements;
                } else {
                    DockerRightPadded<T> element = DockerRightPadded.build(elementCreator.apply(currentElement.toString()));
                    if (appendRightPadding) {
                        element = element.withAfter(rightPadding);
                    }
                    elements.add(element);
                }
            }

            return elements;
        }

        private Docker.Literal createLiteral(String s) {
            if (s == null || s.isEmpty()) {
                return null;
            }
            StringWithPadding stringWithPadding = asStringWithPrefix(s);
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

        private Docker.Option createOption(String s) {
            String[] parts = s.split("=", 2);
            String key = parts.length > 0 ? parts[0] : "";
            String value = parts.length > 1 ? parts[1] : "";
            return new Docker.Option(Tree.randomId(), prefix, key, List.of(stringToKeyArgs(value)), Markers.EMPTY);
        }

        Docker.Instruction parse() {
            String name = instructionType.getSimpleName();
            if (name.equals(Docker.Add.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Add(Tree.randomId(), prefix, Markers.EMPTY, null, null, null);
            } else if (name.equals(Docker.Arg.class.getSimpleName())) {
                List<DockerRightPadded<Docker.KeyArgs>> args = parseArgs(instruction.toString());
                return new Docker.Arg(Tree.randomId(), prefix, Markers.EMPTY, args);
            } else if (name.equals(Docker.Cmd.class.getSimpleName())) {
                String content = instruction.toString();
                Form form = Form.SHELL;
                Space execFormPrefix = Space.EMPTY;
                Space execFormSuffix = Space.EMPTY;
                if (content.trim().startsWith("[")) {
                    StringWithPadding stringWithPadding = asStringWithPrefix(content);
                    content = stringWithPadding.content();
                    execFormPrefix = stringWithPadding.prefix();
                    content = content.substring(1, content.length() - 1);
                    form = Form.EXEC;

                    execFormSuffix = rightPadding;
                }
                return new Docker.Cmd(Tree.randomId(), prefix, execFormPrefix, Markers.EMPTY, form, parseLiterals(form, content), execFormSuffix);
            } else if (name.equals(Docker.Comment.class.getSimpleName())) {
                StringWithPadding stringWithPadding = asStringWithPrefix(instruction.toString());

                return new Docker.Comment(Tree.randomId(), prefix, Markers.EMPTY,
                        DockerRightPadded.build(createLiteral(stringWithPadding.content()).withPrefix(stringWithPadding.prefix())).withAfter(rightPadding));
            } else if (name.equals(Docker.Copy.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Copy(Tree.randomId(), prefix, Markers.EMPTY, null, null, null);
            } else if (name.equals(Docker.Directive.class.getSimpleName())) {
                StringWithPadding stringWithPadding = asStringWithPrefix(instruction.toString());

                String[] parts = stringWithPadding.content().split("=", 2);
                String key = parts.length > 0 ? parts[0] : "";
                String value = parts.length > 1 ? parts[1] : "";

                if ( key.equalsIgnoreCase("escape")) {
                    escapeChar = value.charAt(0);
                }

                return new Docker.Directive(Tree.randomId(), prefix, Markers.EMPTY, new DockerRightPadded<>(
                        new Docker.KeyArgs(stringWithPadding.prefix, key, value, true, quoting),
                        rightPadding,
                        Markers.EMPTY
                ));
            } else if (name.equals(Docker.Entrypoint.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Entrypoint(Tree.randomId(), prefix, Markers.EMPTY, null, null);
            } else if (name.equals(Docker.Env.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Env(Tree.randomId(), prefix, Markers.EMPTY, null);
            }  else if (name.equals(Docker.Expose.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Expose(Tree.randomId(), prefix, Markers.EMPTY, null);
            } else if (name.equals(Docker.From.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.From(Tree.randomId(), prefix, Markers.EMPTY, null, null, null, null, null);
            } else if (name.equals(Docker.Healthcheck.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Healthcheck(Tree.randomId(), prefix, Markers.EMPTY, null, null, null);
            } else if (name.equals(Docker.Label.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Label(Tree.randomId(), prefix, Markers.EMPTY, null);
            } else if (name.equals(Docker.Maintainer.class.getSimpleName())) {
                return new Docker.Maintainer (Tree.randomId(), prefix, Markers.EMPTY, instruction.toString(), quoting);
            } else if (name.equals(Docker.OnBuild.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.OnBuild(Tree.randomId(), prefix, Markers.EMPTY, null);
            }  else if (name.equals(Docker.Run.class.getSimpleName())) {
                List<String> commands = new ArrayList<>();
                if (instruction.toString().contains(escapeChar + "\n")) {
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
            } else if (name.equals(Docker.Shell.class.getSimpleName())) {
                List<String> commands = new ArrayList<>();
                if (instruction.toString().contains(escapeChar + "\n")) {
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

                return new Docker.Shell(Tree.randomId(), prefix, Markers.EMPTY, commands);
            }else if (name.equals(Docker.StopSignal.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.StopSignal(Tree.randomId(), prefix, Markers.EMPTY, null);
            } else if (name.equals(Docker.User.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.User(Tree.randomId(), prefix, Markers.EMPTY, null, null);
            } else if (name.equals(Docker.Volume.class.getSimpleName())) {
                return new Docker.Volume(Tree.randomId(), prefix, Markers.EMPTY, instruction.toString());
            } else if (name.equals(Docker.Workdir.class.getSimpleName())) {
                // TODO: implement this
                return new Docker.Workdir(Tree.randomId(), prefix, Markers.EMPTY, null);
            }
            return null;
        }

        // TODO: maybe have this return a string with prefix and suffix whitespace
        private @NotNull DockerParser.InstructionParser.StringWithPadding asStringWithPrefix(String content) {
            int idx = 0;
            for (char c : content.toCharArray()) {
                if (c == ' ' || c == '\t') {
                    idx++;
                } else {
                    break;
                }
            }

            Space rightPadding = Space.EMPTY;
            Space before = Space.build(content.substring(0, idx));
            content = content.substring(idx);

            idx = content.length() - 1;
            // walk line backwards to find the last non-whitespace character
            for (int i = content.length() - 1; i >= 0; i--) {
                if (content.charAt(i) != ' ' && content.charAt(i) != '\t') {
                    // move the pointer to after the current non-whitespace character
                    idx = i + 1;
                    break;
                }
            }

            if (idx < content.length()) {
                rightPadding = Space.build(content.substring(idx));
                content = content.substring(0, idx);
            }

            return new StringWithPadding(content, before, rightPadding);
        }

        private record StringWithPadding(String content, Space prefix, Space suffix) { }
    }

    public Docker parse(InputStream input) {
        // scan the input stream and maintain state. A newline is the delimiter for a complete instruction unless escaped.
        // when a complete instruction is found, parse it into an AST node
        List<Docker.Stage> stages = new ArrayList<>();
        List<Docker.Instruction> currentInstructions = new ArrayList<>();

        Space eof = Space.EMPTY;
        List<Docker.Instruction> parsed = new ArrayList<>();
        InstructionParser parser = new InstructionParser();

        try (Scanner scanner = new Scanner(input)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) {
                    // tracks newlines for continuations and prefixes
                    parser.appendPrefix(Space.build("\n"));
                    continue;
                }

                // drain the line of any leading whitespace, storing in parser.addPrefix, then inspect the first "word" to determine the instruction type
                while (line.startsWith(" ") || line.startsWith("\t")) {
                    parser.appendPrefix(Space.build(line.substring(0, 1)));
                    line = line.substring(1);
                }

                if (line.isEmpty()) {
                    continue;
                }

                // take line until the first space character
                int spaceIndex = line.indexOf(' ');
                String firstWord = (spaceIndex == -1) ? line : line.substring(0, spaceIndex);

                parser.instructionType = instructionFromText(firstWord);

                // remove the first word from line
                line = (spaceIndex == -1) ? line : line.substring(spaceIndex);

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

                String escapeChar = parser.getEscapeChar();

                if (parser.instructionType == Docker.Comment.class) {
                    String lower = line.toLowerCase().trim();
                    // hack: if comment is a directive, change the type accordingly
                    // directives are used so rarely that I don't care to make this much more robust atm
                    if ((lower.startsWith("syntax=") || lower.startsWith("escape=") || lower.startsWith("check=")) && !lower.contains(" ")) {
                        parser.instructionType = Docker.Directive.class;
                    }
                }

                parser.append(line);
                if (line.endsWith(escapeChar)) {
                    parser.append("\n");
                    continue;
                }
                Docker.Instruction instr = parser.parse();
                currentInstructions.add(instr);
                if (instr instanceof Docker.From) {
                    stages.add(new Docker.Stage(Tree.randomId(), Markers.EMPTY, currentInstructions));
                    currentInstructions.clear();
                }

                parser.reset();
            }
        }

        if (stages.isEmpty()) {
            stages.add(new Docker.Stage(Tree.randomId(), Markers.EMPTY, currentInstructions));
        }

        return new Docker.Document(Tree.randomId(), Markers.EMPTY, Paths.get("Dockerfile"), null, null, false, null, stages, eof);
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
            default -> Docker.Comment.class;
        };
    }
}

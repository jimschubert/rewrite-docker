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
package com.github.jimschubert.rewrite.docker.tree;

import com.github.jimschubert.rewrite.docker.DockerVisitor;
import lombok.*;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.jimschubert.rewrite.docker.internal.StringUtil.trimDoubleQuotes;
import static com.github.jimschubert.rewrite.docker.internal.StringUtil.trimSingleQuotes;

// TODO: maybe revisit some of the types here to determine if any can be simplified (e.g. DockerRightPadded<Literal>)
// TODO: maybe add helper methods to simplify setting of (most) fields?

/**
 * A Dockerfile AST.
 */
public interface Docker extends Tree {

    interface Instruction extends Docker {
        <T extends Tree> T withEol(Space eol);

        Space getEol();
    }


    static boolean same(String first, String second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.equals(second);
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptDocker(v.adapt(DockerVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(DockerVisitor.class);
    }

    @Nullable
    default <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    /**
     * @return A copy of this Dockerfile with the same content, but with new ids.
     */
    Docker copyPaste();

    default Space getPrefix() {
        return Space.EMPTY;
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements Docker {
        @EqualsAndHashCode.Include(rank = 0)
        UUID id;

        Quoting quoting;

        Space prefix;

        @EqualsAndHashCode.Include(rank = 1000)
        String text;

        Space trailing;

        Markers markers;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Literal(Tree.randomId(), quoting, prefix, text, trailing,
                    markers == null ? Markers.EMPTY : Markers.build(markers.getMarkers()));
        }

        public static Literal build(String text) {
            Quoting quoting = Quoting.UNQUOTED;
            if (text != null && text.length() > 1) {
                if (text.startsWith("'") && text.endsWith("'")) {
                    text = trimSingleQuotes(text);
                    quoting = Quoting.SINGLE_QUOTED;
                } else if (text.startsWith("\"") && text.endsWith("\"")) {
                    text = trimDoubleQuotes(text);
                    quoting = Quoting.DOUBLE_QUOTED;
                }
            }
            return new Literal(Tree.randomId(), quoting, Space.EMPTY, text, Space.EMPTY, Markers.EMPTY);
        }

        public static Literal prepend(String text, Literal literal) {
            if (literal == null) {
                return build(text);
            }

            if (text == null || text.isEmpty()) {
                return literal;
            }

            return new Literal(Tree.randomId(), literal.getQuoting(), literal.getPrefix(), text + literal.getText(), literal.getTrailing(), literal.getMarkers());
        }

        public static Literal build(Quoting quoting, Space prefix, String text, Space trailing) {
            return new Literal(Tree.randomId(), quoting, prefix, text, trailing, Markers.EMPTY);
        }
    }

    /**
     * A Dockerfile option, such as --platform or --chown.
     * This is different from KeyArgs, which is intended to be a hashable key-value pair.
     * We use Option to allow for key-value pairs which can be repeated (e.g. --exclude in COPY/ADD).
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Option implements Docker {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;

        KeyArgs keyArgs;

        Markers markers;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitOption(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Option(Tree.randomId(), prefix, keyArgs, markers == null ? Markers.EMPTY : Markers.build(markers.getMarkers()));
        }

        public static Option build(String key, String value) {
            return new Option(Tree.randomId(), Space.EMPTY, KeyArgs.build(key, value).withHasEquals(true), Markers.EMPTY);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @With
    class Document implements Docker, SourceFileWithReferences {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;

        @Nullable
        FileAttributes fileAttributes;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        boolean charsetBomMarked;

        @Nullable
        Checksum checksum;

        List<Stage> stages;

        Space eof;

        Markers markers;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }


        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Document(Tree.randomId(), sourcePath, fileAttributes, charsetName, charsetBomMarked, checksum,
                    stages.stream().map(Stage::copyPaste).collect(Collectors.toCollection(ArrayList::new)), eof, markers);
        }

        public static Document build(Instruction... instructions) {
            Stage stage = new Stage(Tree.randomId(), Arrays.stream(instructions).collect(Collectors.toCollection(ArrayList::new)), Markers.EMPTY);
            return new Document(Tree.randomId(), Path.of("Dockerfile"), null, StandardCharsets.UTF_8.name(), false, null,
                    List.of(stage), Space.EMPTY, Markers.EMPTY);
        }

        public static Document build(List<Stage> stages) {
            return new Document(Tree.randomId(), Path.of("Dockerfile"), null, StandardCharsets.UTF_8.name(), false, null,
                    stages, Space.EMPTY, Markers.EMPTY);
        }

        @Nullable
        @NonFinal
        transient SoftReference<References> references;

        @Override
        public @NonNull References getReferences() {
            this.references = build(references);
            return Objects.requireNonNull(this.references.get());
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Add implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        List<DockerRightPadded<Option>> options;

        List<DockerRightPadded<Literal>> sources;
        DockerRightPadded<Literal> destination;

        Markers markers;

        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitAdd(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Add(Tree.randomId(), prefix, options, sources, destination, markers, eol);
        }

        // todo: builder function?
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Arg implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        List<DockerRightPadded<KeyArgs>> args;

        Markers markers;

        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitArg(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Arg(Tree.randomId(), prefix, args, markers, eol);
        }

        public static Arg build(String key, String value) {
            return new Arg(Tree.randomId(), Space.EMPTY, List.of(
                    DockerRightPadded.build(new KeyArgs(Space.build(" "), Literal.build(key), Literal.build(value), true, Quoting.UNQUOTED))
            ), Markers.EMPTY, Space.build("\n"));
        }

        public static Arg build(KeyArgs... args) {
            return new Arg(Tree.randomId(), Space.EMPTY, Arrays.stream(args)
                    .map(DockerRightPadded::build)
                    .collect(Collectors.toCollection(ArrayList::new)), Markers.EMPTY, Space.build("\n"));
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Cmd implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;
        Form form;
        Space prefix;
        Space execFormPrefix;
        List<DockerRightPadded<Literal>> commands;
        Space execFormSuffix;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitCmd(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Cmd(Tree.randomId(), form, prefix, execFormPrefix, commands, execFormSuffix, markers, eol);
        }

        public static Cmd build(String... commands) {
            return build(Form.EXEC, commands);
        }

        public static Cmd build(Form form, String... commands) {
            return new Cmd(Tree.randomId(), form, Space.EMPTY,
                    Space.build(" "),
                    Arrays.stream(commands)
                            .map(s -> Literal.build(s)
                                    .withQuoting(form == Form.EXEC ? Quoting.DOUBLE_QUOTED : Quoting.UNQUOTED)
                                    .withPrefix(form == Form.EXEC ? Space.EMPTY : Space.build(" "))
                                    .withTrailing(Space.EMPTY)
                            )
                            .map(DockerRightPadded::build)
                            .collect(Collectors.toCollection(ArrayList::new)),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Comment implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        DockerRightPadded<Literal> text;

        Markers markers;

        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Comment(Tree.randomId(), prefix, text, markers, eol);
        }

        public static Comment build(String text) {
            return new Comment(Tree.randomId(),
                    Space.EMPTY,
                    DockerRightPadded.build(Literal.build(text).withPrefix(Space.build(" "))),
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Copy implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        List<DockerRightPadded<Option>> options;
        List<DockerRightPadded<Literal>> sources;
        DockerRightPadded<Literal> destination;

        Markers markers;

        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitCopy(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Copy(Tree.randomId(), prefix, new ArrayList<>(options), sources, destination, markers, eol);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Directive implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        DockerRightPadded<KeyArgs> directive;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitDirective(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Directive(Tree.randomId(), prefix, directive, markers, eol);
        }

        public String getKey() {
            return directive.getElement().key();
        }

        public Directive withKey(String key) {
            if (same(directive.getElement().key(), key)) {
                return this;
            }

            return this.withDirective(directive.map(d -> d.key(key)));
        }

        public Directive withValue(String value) {
            if (same(directive.getElement().value(), value)) {
                return this;
            }

            return this.withDirective(directive.map(d -> d.value(value)));
        }

        public String getValue() {
            return directive.getElement().value();
        }

        public String getFullDirective() {
            if (directive.getElement().getKey() == null) {
                return "";
            }

            return directive.getElement().getKey() + "=" + directive.getElement().getValue();
        }

        public static Directive build(String key, String value) {
            return new Directive(Tree.randomId(),
                    Space.EMPTY,
                    DockerRightPadded.build(
                            new KeyArgs(Space.build(" "), Literal.build(key), Literal.build(value), true, Quoting.UNQUOTED)
                    ),
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Entrypoint implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;
        Form form;
        Space prefix;
        Space execFormPrefix;
        List<DockerRightPadded<Literal>> commands;
        Space execFormSuffix;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitEntrypoint(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Entrypoint(Tree.randomId(), form, prefix, execFormPrefix, commands, execFormSuffix, markers, eol);
        }

        public static Entrypoint build(String... commands) {
            return build(Form.EXEC, commands);
        }

        public static Entrypoint build(Form form, String... commands) {
            return new Entrypoint(Tree.randomId(),
                    form,
                    Space.EMPTY,
                    form == Form.EXEC ? Space.build(" ") : Space.EMPTY,
                    Arrays.stream(commands)
                            .map(s -> Literal.build(s)
                                    .withQuoting(form == Form.EXEC ? Quoting.DOUBLE_QUOTED : Quoting.UNQUOTED)
                                    .withPrefix(form == Form.EXEC ? Space.EMPTY : Space.build(" "))
                                    .withTrailing(Space.EMPTY)
                            )
                            .map(DockerRightPadded::build)
                            .collect(Collectors.toCollection(ArrayList::new)),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Env implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        List<DockerRightPadded<KeyArgs>> args;

        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitEnv(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Env(Tree.randomId(), prefix, args, markers, eol);
        }

        public static Env build(String key, String value) {
            return new Env(Tree.randomId(),
                    Space.EMPTY,
                    List.of(
                            DockerRightPadded.build(
                                    new KeyArgs(Space.build(" "), Literal.build(key), Literal.build(value), true, Quoting.UNQUOTED)
                            )
                    ),
                    Markers.EMPTY,
                    Space.build("\n"));
        }

        public static Env build(KeyArgs... args) {
            return new Env(Tree.randomId(), Space.EMPTY, Arrays.stream(args)
                    .map(arg -> {
                        if ("".equals(arg.getPrefix().getWhitespace())) {
                            return arg.withPrefix(Space.build(" "));
                        }
                        return arg;
                    })
                    .map(DockerRightPadded::build)
                    .collect(Collectors.toCollection(ArrayList::new)),
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Expose implements Docker.Instruction {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        List<DockerRightPadded<Port>> ports;

        Markers markers;

        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitExpose(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Expose(Tree.randomId(), prefix, ports, markers, eol);
        }

        public static Expose build(String... ports) {
            List<DockerRightPadded<Port>> portsList = new ArrayList<>();

            for (String port : ports) {
                String normalizedPort = null;
                String protocol = null;
                if (port != null && port.contains("/")) {
                    String[] parts = port.split("/");
                    protocol = parts[1];
                    normalizedPort = parts[0];
                } else {
                    normalizedPort = port;
                }

                portsList.add(DockerRightPadded.build(
                        new Port(Space.build(" "),
                                normalizedPort,
                                protocol == null ? "tcp" : protocol,
                                protocol != null)));
            }

            return new Expose(Tree.randomId(),
                    Space.EMPTY,
                    portsList,
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class From implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        DockerRightPadded<Literal> platform;
        DockerRightPadded<Literal> image;
        DockerRightPadded<Literal> version;
        Literal as;
        DockerRightPadded<Literal> alias;
        Space trailing;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitFrom(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new From(Tree.randomId(), prefix, platform, image, version, as, alias, trailing, markers, eol);
        }

        public String getImageSpec() {
            return image.getElement().getText();
        }

        public String getImageSpecWithVersion() {
            if (version.getElement() == null || version.getElement().getText() == null) {
                return image.getElement().getText();
            }
            return image.getElement().getText() + version.getElement().getText();
        }

        public String getDigest() {
            String v = version.getElement().getText();
            if (v == null) {
                return null;
            }
            return v.startsWith("@") ? v.substring(1) : null;
        }

        public From platform(String platform) {
            return this.withPlatform(this.platform.map(p -> p.withText(platform)));
        }

        public From image(String image) {
            return this.withImage(this.image.map(i -> i.withText(image)));
        }

        public From version(String version) {
            return this.withVersion(this.version.map(v -> v.withText(version)));
        }

        public From withDigest(String digest) {
            if (digest == null) {
                return version(null);
            }
            digest = digest.indexOf('@') == 0 ? digest : "@" + digest;
            return version(digest);
        }

        public From withTag(String tag) {
            if (tag == null) {
                return version(null);
            }
            tag = tag.indexOf(':') == 0 ? tag : ":" + tag;
            return version(tag);
        }

        public String getTag() {
            String v = version.getElement().getText();
            if (v == null) {
                return null;
            }

            return v.startsWith(":") ? v.substring(1) : null;
        }

        public From alias(String alias) {
            From result = this;
            if (this.as.getText() == null || this.as.getText().isBlank()) {
                result = result.withAs(Literal.build("AS").withPrefix(this.as.getPrefix().map(p -> p == null || p.isEmpty() ? " " : p)));
            }
            return result.withAlias(this.alias.map(a -> a.withText(alias).withPrefix(a.getPrefix().map(p -> p == null || p.isEmpty() ? " " : p))));
        }

        public static From build(String image) {
            return new From(Tree.randomId(), Space.EMPTY,
                    DockerRightPadded.build(Literal.build(null).withPrefix(Space.build(" "))),
                    DockerRightPadded.build(Literal.build(null).withPrefix(Space.build(" "))),
                    DockerRightPadded.build(Literal.build(null).withPrefix(Space.build(" "))),
                    Literal.build(null).withPrefix(Space.build(" ")),
                    DockerRightPadded.build(Literal.build(null).withPrefix(Space.build(" "))),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Space.build("\n")).image(image);
        }

        public static From build(String prefix, String platform, String image, String version, String alias) {
            return new From(Tree.randomId(), Space.build(prefix),
                    DockerRightPadded.build(Literal.build(null).withPrefix(Space.build(" "))),
                    DockerRightPadded.build(Literal.build(null).withPrefix(Space.build(" "))),
                    DockerRightPadded.build(Literal.build(null).withPrefix(Space.build(" "))),
                    alias != null && !alias.isBlank() ? Literal.build("AS").withPrefix(Space.build(" ")) : null,
                    DockerRightPadded.build(Literal.build(alias).withPrefix(Space.build(" "))),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Space.build("\n")
            )
                    .image(image)
                    .version(version)
                    .platform(platform);
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Healthcheck implements Docker.Instruction {
        public enum Type {
            CMD, NONE
        }

        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Type type;

        List<DockerRightPadded<KeyArgs>> options;
        List<DockerRightPadded<Literal>> commands;

        Markers markers;

        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitHealthcheck(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Healthcheck(Tree.randomId(),
                    prefix,
                    type,
                    options == null ? new ArrayList<>() : new ArrayList<>(options),
                    commands == null ? new ArrayList<>() : new ArrayList<>(commands),
                    markers,
                    eol);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Label implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        List<DockerRightPadded<KeyArgs>> args;

        Markers markers;

        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitLabel(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Label(Tree.randomId(), prefix, args, markers, eol);
        }

        public static Label build(String key, String value) {
            return new Label(Tree.randomId(), Space.EMPTY, List.of(
                    DockerRightPadded.build(new KeyArgs(Space.build(" "), Literal.build(key), Literal.build(value), true, Quoting.UNQUOTED))
            ), Markers.EMPTY, Space.build("\n"));
        }

        public static Label build(KeyArgs... args) {
            return new Label(Tree.randomId(), Space.EMPTY, Arrays.stream(args)
                    .map(DockerRightPadded::build)
                    .collect(Collectors.toCollection(ArrayList::new)), Markers.EMPTY, Space.build("\n"));
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Maintainer implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;
        Quoting quoting;
        Space prefix;
        Literal name;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitMaintainer(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Maintainer(Tree.randomId(), quoting, prefix, name, markers, eol);
        }

        public static Maintainer build(String name) {
            if (name == null) {
                return null;
            }

            Quoting quoting = Quoting.UNQUOTED;
            if (name.startsWith("'") && name.endsWith("'")) {
                name = trimSingleQuotes(name);
                quoting = Quoting.SINGLE_QUOTED;
            } else if (name.startsWith("\"") && name.endsWith("\"")) {
                name = trimDoubleQuotes(name);
                quoting = Quoting.DOUBLE_QUOTED;
            }

            return new Maintainer(Tree.randomId(), quoting, Space.EMPTY,
                    Literal.build(name).withPrefix(Space.build(" ")),
                    Markers.EMPTY, Space.build("\n"));
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class OnBuild implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Docker instruction;
        Space trailing;

        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitOnBuild(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new OnBuild(Tree.randomId(), prefix, instruction.copyPaste(), trailing, markers, eol);
        }

        public static OnBuild build(Docker instruction) {
            return new OnBuild(Tree.randomId(), Space.EMPTY, instruction, Space.EMPTY, Markers.EMPTY, Space.build("\n"));
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Run implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        List<DockerRightPadded<Option>> options;
        List<DockerRightPadded<Literal>> commands;

        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitRun(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Run(Tree.randomId(), prefix, options, commands, markers, eol);
        }

        public static Run build(String... commands) {
            return new Run(Tree.randomId(),
                    Space.EMPTY,
                    null,
                    Arrays.stream(commands)
                            .map(s -> Literal.build(s).withPrefix(Space.build(" ")))
                            .map(DockerRightPadded::build)
                            .collect(Collectors.toList()),
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Shell implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Space execFormPrefix;
        List<DockerRightPadded<Literal>> commands;
        Space execFormSuffix;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitShell(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Shell(Tree.randomId(), prefix, execFormPrefix, commands, execFormSuffix, markers, eol);
        }

        public static Shell build(String... commands) {
            return new Shell(Tree.randomId(),
                    Space.EMPTY,
                    Space.build(" "),
                    Arrays.stream(commands)
                            .map(Literal::build)
                            .map(DockerRightPadded::build)
                            .collect(Collectors.toList()),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StopSignal implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Literal signal;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitStopSignal(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new StopSignal(Tree.randomId(), prefix, signal, markers, eol);
        }

        public static StopSignal build(String signal) {
            return new StopSignal(Tree.randomId(),
                    Space.EMPTY,
                    Literal.build(signal).withPrefix(Space.build(" ")),
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class User implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Literal username;
        Literal group;

        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitUser(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new User(Tree.randomId(), prefix, username, group, markers, eol);
        }

        public static User build(String username) {
            return build(username, null);
        }

        public static User build(String username, String group) {
            return new User(Tree.randomId(), Space.EMPTY,
                    username == null ? null : Literal.build(username).withPrefix(Space.build(" ")),
                    group == null ? null : Literal.build(group).withPrefix(Space.build(" ")),
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Volume implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Form form;

        Space prefix;

        Space execFormPrefix;
        List<DockerRightPadded<Literal>> paths;
        Space execFormSuffix;

        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitVolume(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Volume(Tree.randomId(), form, prefix, execFormPrefix, paths, execFormSuffix, markers, eol);
        }


        public static Volume build(String... commands) {
            return build(Form.EXEC, commands);
        }

        public static Volume build(Form form, String... commands) {
            return new Volume(Tree.randomId(),
                    form,
                    Space.EMPTY,
                    form == Form.EXEC ? Space.build(" ") : Space.EMPTY,
                    Arrays.stream(commands)
                            .map(s -> Literal.build(s)
                                    .withQuoting(form == Form.EXEC ? Quoting.DOUBLE_QUOTED : Quoting.UNQUOTED)
                                    .withPrefix(form == Form.EXEC ? Space.EMPTY : Space.build(" "))
                                    .withTrailing(Space.EMPTY)
                            )
                            .map(DockerRightPadded::build)
                            .collect(Collectors.toCollection(ArrayList::new)),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stage implements Docker {
        @EqualsAndHashCode.Include
        UUID id;

        List<Docker> children;

        Markers markers;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitStage(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Stage copyPaste() {
            return new Stage(Tree.randomId(), children, markers);
        }

        public static Stage build(Instruction... instructions) {
            return new Stage(Tree.randomId(),
                    Arrays.stream(instructions).collect(Collectors.toCollection(ArrayList::new)),
                    Markers.EMPTY);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Workdir implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Literal path;
        Markers markers;
        Space eol;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitWorkdir(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Workdir(Tree.randomId(), prefix, path, markers, eol);
        }

        public static Workdir build(String path) {
            return new Workdir(Tree.randomId(),
                    Space.EMPTY,
                    Literal.build(path).withPrefix(Space.build(" ")),
                    Markers.EMPTY,
                    Space.build("\n"));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @With
    class KeyArgs {
        Space prefix;
        @EqualsAndHashCode.Include(rank = 1000)
        Literal key;
        @EqualsAndHashCode.Include(rank = 1000)
        Literal value;
        boolean hasEquals;
        Quoting quoting;

        public String key() {
            return key.getText();
        }

        public String value() {
            return value.getText();
        }

        public KeyArgs key(String key) {
            return this.withKey(Literal.build(key));
        }

        public KeyArgs value(String value) {
            return this.withValue(Literal.build(value));
        }

        public static KeyArgs build(String key, String value) {
            return build(key, value, true);
        }

        public static KeyArgs build(String key, String value, boolean hasEquals) {
            if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
                return new KeyArgs(Space.build(" "), Literal.build(key), Literal.build(trimDoubleQuotes(value)), hasEquals, Quoting.DOUBLE_QUOTED);
            } else if (value != null && value.startsWith("'")) {
                return new KeyArgs(Space.build(" "), Literal.build(key), Literal.build(trimSingleQuotes(value)), hasEquals, Quoting.SINGLE_QUOTED);
            }

            return new KeyArgs(Space.build(" "), Literal.build(key), Literal.build(value), hasEquals, Quoting.UNQUOTED);
        }
    }

//    @Value
//    @With
//    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
//    class SecurityOption {
//        Space prefix;
//        String name;
//    }

    @Value
    @With
    @EqualsAndHashCode
    class Port {
        Space prefix;
        String port;
        String protocol;
        boolean protocolProvided;
    }

//    @Value
//    @With
//    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
//    class NetworkOption {
//        Space prefix;
//        String name;
//    }

//    // TODO: Extends Dockerfile, add visitor
//    @Value
//    @With
//    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
//    class Mount{
//        Space prefix;
//        String type;
//        List<KeyArgs> options;
//        String id;
//    }
}

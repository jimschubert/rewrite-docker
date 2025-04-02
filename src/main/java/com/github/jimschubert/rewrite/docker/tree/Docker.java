package com.github.jimschubert.rewrite.docker.tree;

import com.github.jimschubert.rewrite.docker.DockerVisitor;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public interface Docker extends Tree {

    interface Instruction extends Docker {}


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
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        String text;

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
            return new Literal(Tree.randomId(), prefix, text, markers == null ? Markers.EMPTY : Markers.build(markers.getMarkers()));
        }

        public static Literal build(String text) {
            return new Literal(Tree.randomId(), Space.EMPTY, text, Markers.EMPTY);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Option implements Docker {
        @EqualsAndHashCode.Include
        UUID id;

        String name;
        List<KeyArgs> keyArgs;

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
            return new Option(Tree.randomId(), name, keyArgs, markers == null ? Markers.EMPTY : Markers.build(markers.getMarkers()));
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Document implements Docker, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
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
            return v.defaultValue(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Document(Tree.randomId(), markers, sourcePath, fileAttributes, charsetName, charsetBomMarked, checksum,
                    stages.stream().map(Stage::copyPaste).collect(Collectors.toList()), eof);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Add implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<DockerRightPadded<Option>> options;

        List<DockerRightPadded<Literal>> sources;
        DockerRightPadded<Literal> destination;

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
            return new Add(Tree.randomId(), prefix, markers, options, sources, destination);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Arg implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<DockerRightPadded<KeyArgs>> args;

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
            return new Arg(Tree.randomId(), prefix, markers, args);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Cmd implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Form form;
        List<DockerRightPadded<Literal>> commands;

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
            return new Cmd(Tree.randomId(), prefix, markers, form, commands);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Comment implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        DockerRightPadded<Literal> text;

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
            return new Comment(Tree.randomId(), prefix, markers, text);
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Copy implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<DockerRightPadded<Literal>> sources;
        DockerRightPadded<Literal> destination;

        List<DockerRightPadded<Option>> options;

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
            return new Copy(Tree.randomId(), prefix, markers, sources, destination, new ArrayList<>(options));
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Directive implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        DockerRightPadded<KeyArgs> directive;

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
            return new Directive(Tree.randomId(), prefix, markers, directive);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Entrypoint implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<DockerRightPadded<Literal>> command;

        Space trailing;

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
            return new Entrypoint(Tree.randomId(), prefix, markers, command, trailing);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Env implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<DockerRightPadded<KeyArgs>> args;

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
            return new Env(Tree.randomId(), prefix, markers, args);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Expose implements Docker.Instruction {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<DockerRightPadded<Port>> ports;

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
            return new Expose(Tree.randomId(), prefix, markers, ports);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class From implements Docker.Instruction {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @NonFinal
        DockerRightPadded<Literal> platform;

        @NonFinal
        DockerRightPadded<Literal> image;

        @NonFinal
        DockerRightPadded<Literal> version;

        @With
        Literal as;

        @NonFinal
        DockerRightPadded<Literal> alias;

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
            return new From(Tree.randomId(), prefix, markers, platform, image, version, as, alias);
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
            return v.startsWith("@") ? v.substring(1) : null;
        }

        public From withPlatform(String platform) {
            Space prefix = this.platform.getElement() == null ? Space.EMPTY : this.platform.getElement().getPrefix();
            this.platform = this.platform.withElement(Literal.build(platform).withPrefix(prefix).withMarkers(Markers.EMPTY));
            return this;
        }

        public From withImage(String image) {
            Space prefix = this.image.getElement() == null ? Space.build(" ") : this.image.getElement().getPrefix();
            this.image = this.image.withElement(Literal.build(image).withPrefix(prefix).withMarkers(Markers.EMPTY));
            return this;
        }

        public From withVersion(String version) {
            this.version = this.version.withElement(Literal.build(version).withMarkers(Markers.EMPTY));
            return this;
        }

        public From withDigest(String digest) {
            if (digest == null) {
                return this;
            }
            digest = digest.indexOf('@') == 0 ? digest : "@" + digest;
            version = version.withElement(Literal.build(digest).withMarkers(Markers.EMPTY));
            return this;
        }

        public From withTag(String tag) {
            if (tag == null) {
                return this;
            }
            tag = tag.indexOf(':') == 0 ? tag : ":" + tag;
            version = version.withElement(Literal.build(tag).withMarkers(Markers.EMPTY));
            return this;
        }

        public String getTag() {
            String v = version.getElement().getText();
            return v.startsWith(":") ? v.substring(1) : null;
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
        Markers markers;

        Type type;
        String command;
        LinkedHashMap<String, String> options;

        public Healthcheck(UUID id, Space prefix, Markers markers, Type type, String command, LinkedHashMap<String, String> options) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.type = type;
            this.command = command;
            this.options = new LinkedHashMap<>(options);
        }

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
            return new Healthcheck(Tree.randomId(), prefix, markers, type, command, new LinkedHashMap<>(options));
        }

        public LinkedHashMap<String, String> getOptions() {
            return new LinkedHashMap<>(options);
        }

        public void clearOptions() {
            this.options.clear();
        }

        public void addOption(String key, String value) {
            this.options.put(key, value);
        }

        public void removeOption(String key) {
            this.options.remove(key);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Label implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<KeyArgs> args;

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
            return new Label(Tree.randomId(), prefix, markers, args);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Maintainer implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String name;
        Quoting quoting;

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
            return new Maintainer(Tree.randomId(), prefix, markers, name, quoting);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class OnBuild implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Docker instruction;

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
            return new OnBuild(Tree.randomId(), prefix, markers, instruction.copyPaste());
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Run implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<String> commands;
        List<Mount> mounts;
        NetworkOption networkOption;
        SecurityOption securityOption;
        String heredoc;
        String heredocName;

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
            return new Run(Tree.randomId(), prefix, markers, commands, mounts, networkOption, securityOption, heredoc, heredocName);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Shell implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<String> commands;

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
            return new Shell(Tree.randomId(), prefix, markers, commands);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StopSignal implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String signal;

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
            return new StopSignal(Tree.randomId(), prefix, markers, signal);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class User implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String username;
        String group;

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
            return new User(Tree.randomId(), prefix, markers, username, group);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Volume implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String path;

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
            return new Volume(Tree.randomId(), prefix, markers, path);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stage implements Docker {
        @EqualsAndHashCode.Include
        UUID id;
        Markers markers;

        List<Instruction> children;

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
            return new Stage(Tree.randomId(), markers, children);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Workdir implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String path;

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
            return new Workdir(Tree.randomId(), prefix, markers, path);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class KeyArgs {
        Space prefix;
        @EqualsAndHashCode.Include
        String key;
        @EqualsAndHashCode.Include
        String value;
        boolean hasEquals;
        Quoting quoting;
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class SecurityOption {
        Space prefix;
        String name;
    }

    @Value
    @With
    @EqualsAndHashCode
    class Port {
        Space prefix;
        String port;
        String protocol;
        boolean protocolProvided;
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class NetworkOption {
        Space prefix;
        String name;
    }

    // TODO: Extends Dockerfile, add visitor
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Mount{
        Space prefix;
        String type;
        List<KeyArgs> options;
        String id;
    }
}

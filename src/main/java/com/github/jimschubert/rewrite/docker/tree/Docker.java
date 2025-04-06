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

// TODO: maybe revisit some of the types here to determine if any can be simplified (e.g. DockerRightPadded<Literal>)
// TODO: maybe add helper methods to simplify setting of (most) fields?
/**
 * A Dockerfile AST.
 */
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

        Quoting quoting;

        Space prefix;

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
            return new Literal(Tree.randomId(), Quoting.UNQUOTED, Space.EMPTY, text, Space.EMPTY, Markers.EMPTY);
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
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Document implements Docker, SourceFile {
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
            return v.defaultValue(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Docker copyPaste() {
            return new Document(Tree.randomId(), sourcePath, fileAttributes, charsetName, charsetBomMarked, checksum,
                    stages.stream().map(Stage::copyPaste).collect(Collectors.toList()), eof, markers);
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
            return new Add(Tree.randomId(), prefix, options, sources, destination, markers);
        }
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
            return new Arg(Tree.randomId(), prefix, args, markers);
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
            return new Cmd(Tree.randomId(), form, prefix, execFormPrefix, commands, execFormSuffix, markers);
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
            return new Comment(Tree.randomId(), prefix, text, markers);
        }

        public static Comment build(String text) {
            return new Comment(Tree.randomId(), Space.EMPTY, DockerRightPadded.build(Literal.build(text).withPrefix(Space.build(" "))), Markers.EMPTY);
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
            return new Copy(Tree.randomId(), prefix, new ArrayList<>(options), sources, destination, markers);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Directive implements Docker.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        @NonFinal
        DockerRightPadded<KeyArgs> directive;

        Markers markers;

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
            return new Directive(Tree.randomId(), prefix, directive, markers);
        }

        public String getKey() {
            return directive.getElement().getKey();
        }

        public Directive withKey(String key) {
            Space prefix = directive.getElement().getKey() == null ? Space.EMPTY : directive.getElement().getPrefix();
            directive = directive.withElement(new KeyArgs(prefix, key, directive.getElement().getValue(), directive.getElement().isHasEquals(), directive.getElement().getQuoting()));
            return this;
        }

        public Directive withValue(String value) {
            Space prefix = directive.getElement().getValue() == null ? Space.EMPTY : directive.getElement().getPrefix();
            directive = directive.withElement(new KeyArgs(prefix, directive.getElement().getKey(), value, directive.getElement().isHasEquals(), directive.getElement().getQuoting()));
            return this;
        }

        public String getValue() {
            return directive.getElement().getValue();
        }

        public String getFullDirective() {
            if (directive.getElement().getKey() == null) {
                return "";
            }

            return directive.getElement().getKey() + "=" + directive.getElement().getValue();
        }

        public static Directive build(String key, String value) {
            return new Directive(Tree.randomId(), Space.EMPTY, DockerRightPadded.build(new KeyArgs(Space.build(" "), key, value, true, Quoting.UNQUOTED)), Markers.EMPTY);
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
            return new Entrypoint(Tree.randomId(), form, prefix,execFormPrefix, commands, execFormSuffix, markers);
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
            return new Env(Tree.randomId(), prefix, args, markers);
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
            return new Expose(Tree.randomId(), prefix, ports, markers);
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

        @With
        Space trailing;

        @With
        Markers markers;

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
            return new From(Tree.randomId(), prefix, platform, image, version, as, alias, trailing, markers);
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

        public From withPlatform(String platform) {
            if (this.platform == null) {
                this.platform = DockerRightPadded.build(Literal.build(null));
            }
            this.platform = this.platform.withElement(this.platform.getElement().withText(platform));
            return this;
        }

        public From withImage(String image) {
            if (this.image == null) {
                this.image = DockerRightPadded.build(Literal.build(null));
            }
            this.image = this.image.withElement(this.image.getElement().withText(image));
            return this;
        }

        public From withVersion(String version) {
            if (this.version == null) {
                this.version = DockerRightPadded.build(Literal.build(null));
            }

            this.version = this.version.withElement(this.version.getElement().withText(version));
            return this;
        }

        public From withDigest(String digest) {
            if (digest == null) {
                return withVersion(null);
            }
            digest = digest.indexOf('@') == 0 ? digest : "@" + digest;
            return withVersion(digest);
        }

        public From withTag(String tag) {
            if (tag == null) {
                return withVersion(null);
            }
            tag = tag.indexOf(':') == 0 ? tag : ":" + tag;
            return withVersion(tag);
        }

        public String getTag() {
            String v = version.getElement().getText();
            if (v == null) {
                return null;
            }

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
        Type type;

        @NonFinal
        List<DockerRightPadded<KeyArgs>> options;

        @NonFinal
        List<DockerRightPadded<Literal>> commands;

        Markers markers;

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
                    markers);
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
            return new Label(Tree.randomId(), prefix, args, markers);
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
        String name;
        Markers markers;

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
            return new Maintainer(Tree.randomId(), quoting, prefix, name, markers);
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
            return new OnBuild(Tree.randomId(), prefix, instruction.copyPaste(), trailing, markers);
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
            return new Run(Tree.randomId(), prefix, options, commands, markers);
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
            return new Shell(Tree.randomId(), prefix, execFormPrefix, commands, execFormSuffix, markers);
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
            return new StopSignal(Tree.randomId(), prefix, signal, markers);
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
            return new User(Tree.randomId(), prefix, username, group, markers);
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
            return new Volume(Tree.randomId(), form, prefix, execFormPrefix, paths, execFormSuffix, markers);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stage implements Docker {
        @EqualsAndHashCode.Include
        UUID id;

        List<Instruction> children;

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
            return new Workdir(Tree.randomId(), prefix, path, markers);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class KeyArgs {
        Space prefix;
        @EqualsAndHashCode.Include(rank = 0)
        String key;
        @EqualsAndHashCode.Include(rank = 1)
        String value;
        boolean hasEquals;
        Quoting quoting;
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

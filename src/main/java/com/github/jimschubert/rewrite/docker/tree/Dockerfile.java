package com.github.jimschubert.rewrite.docker.tree;

import com.github.jimschubert.rewrite.docker.DockerfileVisitor;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public interface Dockerfile extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptDocker(v.adapt(DockerfileVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(DockerfileVisitor.class);
    }

    @Nullable
    default <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    /**
     * @return A copy of this Dockerfile with the same content, but with new ids.
     */
    Dockerfile copyPaste();

    default Space getPrefix() {
        return Space.EMPTY;
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Document implements Dockerfile, SourceFile {
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
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.defaultValue(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Document(Tree.randomId(), markers, sourcePath, fileAttributes, charsetName, charsetBomMarked, checksum,
                    stages.stream().map(Stage::copyPaste).collect(Collectors.toList()), eof);
        }
    }

    interface Instruction extends Dockerfile {}

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stage implements Dockerfile {
        @EqualsAndHashCode.Include
        UUID id;
        Markers markers;

        List<Instruction> children;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitStage(this, p); // TODO
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
    class From implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        // TODO: other fields

        String imageSpec;
        @Nullable String alias;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitFrom(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new From(Tree.randomId(), prefix, markers, imageSpec, alias);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Run implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String command;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitRun(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Run(Tree.randomId(), prefix, markers, command);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Cmd implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String command;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitCmd(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Cmd(Tree.randomId(), prefix, markers, command);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Label implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<KeyArgs> args;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitLabel(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Label(Tree.randomId(), prefix, markers, args);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Maintainer implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String name;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitMaintainer(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Maintainer(Tree.randomId(), prefix, markers, name);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Expose implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        // TODO: Custom Port type
        String port;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitExpose(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Expose(Tree.randomId(), prefix, markers, port);
        }
    }


    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Env implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<KeyArgs> args;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitEnv(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Env(Tree.randomId(), prefix, markers, args);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Add implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String source;
        String destination;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitAdd(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Add(Tree.randomId(), prefix, markers, source, destination);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Copy implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String source;
        String destination;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitCopy(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Copy(Tree.randomId(), prefix, markers, source, destination);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Entrypoint implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<String> command;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitEntrypoint(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Entrypoint(Tree.randomId(), prefix, markers, command);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Volume implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String path;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitVolume(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Volume(Tree.randomId(), prefix, markers, path);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class User implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String username;
        String group;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitUser(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new User(Tree.randomId(), prefix, markers, username, group);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Workdir implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String path;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitWorkdir(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Workdir(Tree.randomId(), prefix, markers, path);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Arg implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<KeyArgs> args;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitArg(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Arg(Tree.randomId(), prefix, markers, args);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class OnBuild implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Dockerfile instruction;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitOnBuild(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new OnBuild(Tree.randomId(), prefix, markers, instruction.copyPaste());
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StopSignal implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String signal;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitStopSignal(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new StopSignal(Tree.randomId(), prefix, markers, signal);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Healthcheck implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String command;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitHealthcheck(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Healthcheck(Tree.randomId(), prefix, markers, command);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Shell implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<String> commands;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitShell(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Shell(Tree.randomId(), prefix, markers, commands);
        }
    }
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Comment implements Dockerfile.Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String text;

        @Override
        public <P> Dockerfile acceptDocker(DockerfileVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerfilePrinter<>();
        }

        @Override
        public Dockerfile copyPaste() {
            return new Comment(Tree.randomId(), prefix, markers, text);
        }
    }
}

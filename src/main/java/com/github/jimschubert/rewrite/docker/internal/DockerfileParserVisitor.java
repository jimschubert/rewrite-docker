package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.docker.ast.*;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.Docker.*;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Quoting;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DockerfileParserVisitor {
    private final Path relativePath;
    private final FileAttributes fileAttributes;
    private final Charset charset;

    // TODO: Implement these fields if needed.
    private final EncodingDetectingInputStream source;
    private int cursor = 0;

    public DockerfileParserVisitor(Path relativePath, FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.relativePath = relativePath;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = source.getCharset();
    }

    public Docker.Document visit(List<DockerInstruction> dockerInstructions) {
        List<Docker.Stage> stages = new ArrayList<>();
        List<Docker.Instruction> currentInstructions = new ArrayList<>();

        for (DockerInstruction instruction : dockerInstructions) {
            Docker.Instruction convertedInstruction = convertToDockerfileInstruction(instruction);
            currentInstructions.add(convertedInstruction);
            if (convertedInstruction instanceof From) {
                Stage stage = visitStage(new Docker.Stage(Tree.randomId(), Markers.EMPTY, currentInstructions));
                stages.add(stage);
                currentInstructions = new ArrayList<>();
            }
        }

        if (stages.isEmpty()) {
            Stage stage = visitStage(new Docker.Stage(Tree.randomId(), Markers.EMPTY, currentInstructions));
            stages.add(stage);
        }

        return new Docker.Document(
                Tree.randomId(),
                Markers.EMPTY,
                relativePath,
                fileAttributes,
                charset.name(),
                true,
                null,
                stages,
                Space.EMPTY
        );
    }

    private Docker.Instruction convertToDockerfileInstruction(DockerInstruction instr) {
        switch (instr.getInstruction()) {
            case "ENTRYPOINT":
                return visitEntrypoint((EntrypointInstruction) instr);
            case "VOLUME":
                return visitVolume((VolumeInstruction) instr);
            case "USER":
                return visitUser((UserInstruction) instr);
            case "WORKDIR":
                return visitWorkdir((WorkdirInstruction) instr);
            case "ARG":
                return visitArg((ArgInstruction) instr);
            case "FROM":
                return visitFrom((FromInstruction) instr);
            // Add other cases here
            default:
                throw new IllegalArgumentException("Unknown instruction type: " + instr.getInstruction());
        }
    }

    private Instruction visitFrom(FromInstruction instr) {
        Matcher aliasMatcher = Pattern.compile("^FROM(?:\\s*--platform=[a-z0-9/]+)?\\s+([^ ]+)\\s+(as|AS)\\s+(.+)\\s*$").matcher(instr.toCanonicalForm());
        String image = instr.getImage();
        String as = null;
        String alias = instr.getAlias();
        String platform = instr.getPlatform();
        String digest = instr.getDigest();
        String tag = null;

        // if it's aliased, this parser returns the alias as the whole image (a bug).
        // This is a workaround which will continue to work even if the parser is fixed.
        if (aliasMatcher.matches()) {
            image = aliasMatcher.group(1);
            as = aliasMatcher.group(2);
            alias = aliasMatcher.group(3);
        }

        // This parser doesn't parse a few things properly, so we need to handle them here.
        if (image.indexOf('@') > 0) {
            String[] parts = image.split("@");
            image = parts[0];
            if (parts.length > 1) {
                digest = parts[1];
            }
        } else if (image.indexOf(':') > 0) {
            String[] parts = image.split(":");
            image = parts[0];
            if (parts.length > 1) {
                tag = parts[1];
            }
        }

        return new From(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                DockerRightPadded.build(Literal.build(platform).withPrefix(Space.build(" "))),
                DockerRightPadded.build(Literal.build(image).withPrefix(Space.build(" "))),
                DockerRightPadded.build(Literal.build(null)),
                Literal.build(as).withPrefix(Space.build(" ")),
                DockerRightPadded.build(Literal.build(alias).withPrefix(Space.build(" ")))
        ).withDigest(digest).withTag(tag);
    }

    public Stage visitStage(Stage instr) {
        return instr.copyPaste();
    }

    public Entrypoint visitEntrypoint(EntrypointInstruction instr) {
        return new Entrypoint(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                instr.getEntrypoint().stream().map(e ->
                        DockerRightPadded.build(Literal.build(e).withPrefix(Space.build(" ")))
                                .withAfter(Space.build(" "))
                ).toList(), Space.EMPTY);
    }

    public Volume visitVolume(VolumeInstruction instr) {
        return new Volume(Tree.randomId(), Space.EMPTY, Markers.EMPTY, instr.getVolume());
    }

    public User visitUser(UserInstruction instr) {
        return new User(Tree.randomId(), Space.EMPTY, Markers.EMPTY, instr.getUser(), instr.getGroup());
    }

    public Workdir visitWorkdir(WorkdirInstruction instr) {
        return new Workdir(Tree.randomId(), Space.EMPTY, Markers.EMPTY, instr.getWorkdir());
    }

    public Arg visitArg(ArgInstruction instr) {
        return new Arg(Tree.randomId(), Space.EMPTY, Markers.EMPTY, instr.getArgs().stream()
                .map(kv -> DockerRightPadded.build(new Docker.KeyArgs(Space.build(" "), kv.getKey(), kv.getValue(), kv.hasEquals(), Quoting.valueOf(kv.getQuoting().name()))))
                .collect(Collectors.toList()));
    }

    // Add other visitor methods here
}
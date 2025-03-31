package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.docker.ast.*;
import com.github.jimschubert.rewrite.docker.tree.Dockerfile;
import com.github.jimschubert.rewrite.docker.tree.Dockerfile.*;
import com.github.jimschubert.rewrite.docker.tree.KeyArgs;
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
import java.util.UUID;
import java.util.stream.Collectors;

public class DockerfileParserVisitor {
    private final Path relativePath;
    private final FileAttributes fileAttributes;
    private final Charset charset;
    private final EncodingDetectingInputStream source;
    private int cursor = 0;

    public DockerfileParserVisitor(Path relativePath, FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.relativePath = relativePath;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = source.getCharset();
    }

    public Dockerfile.Document visit(List<DockerInstruction> dockerInstructions) {
        List<Dockerfile.Stage> stages = new ArrayList<>();
        List<Dockerfile.Instruction> currentInstructions = new ArrayList<>();

        for (DockerInstruction instruction : dockerInstructions) {
            Dockerfile.Instruction convertedInstruction = convertToDockerfileInstruction(instruction);
            currentInstructions.add(convertedInstruction);
            if (convertedInstruction instanceof From) {
                Stage stage = visitStage(new Dockerfile.Stage(Tree.randomId(), Markers.EMPTY, currentInstructions));
                stages.add(stage);
                currentInstructions = new ArrayList<>();
            }
        }

        if (stages.isEmpty()) {
            Stage stage = visitStage(new Dockerfile.Stage(Tree.randomId(), Markers.EMPTY, currentInstructions));
            stages.add(stage);
        }

        return new Dockerfile.Document(
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

    private Dockerfile.Instruction convertToDockerfileInstruction(DockerInstruction instr) {
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
        return new From(Tree.randomId(), null, null, instr.getImage(), instr.getAlias());
    }

    public Stage visitStage(Stage instr) {
        return instr.copyPaste();
    }

    public Entrypoint visitEntrypoint(EntrypointInstruction instr) {
        return new Entrypoint(Tree.randomId(), null, null, instr.getEntrypoint());
    }

    public Volume visitVolume(VolumeInstruction instr) {
        return new Volume(Tree.randomId(), null, null, instr.getVolume());
    }

    public User visitUser(UserInstruction instr) {
        return new User(UUID.randomUUID(), null, null, instr.getUser(), instr.getGroup());
    }

    public Workdir visitWorkdir(WorkdirInstruction instr) {
        return new Workdir(UUID.randomUUID(), null, null, instr.getWorkdir());
    }

    public Arg visitArg(ArgInstruction instr) {
        return new Arg(UUID.randomUUID(), null, null, instr.getArgs().stream()
                .map(kv -> new KeyArgs(kv.getKey(), kv.getValue(), kv.hasEquals(), Quoting.valueOf(kv.getQuoting().name())))
                .collect(Collectors.toList()));
    }

    // Add other visitor methods here
}
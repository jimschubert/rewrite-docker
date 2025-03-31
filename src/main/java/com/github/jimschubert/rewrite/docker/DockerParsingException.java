package com.github.jimschubert.rewrite.docker;

import java.nio.file.Path;

public class DockerParsingException extends Exception {
    private final Path sourcePath;
    public DockerParsingException(Path sourcePath, String message, Throwable t) {
        super(message, t);
        this.sourcePath = sourcePath;
    }

    public Path getSourcePath() {
        return sourcePath;
    }
}

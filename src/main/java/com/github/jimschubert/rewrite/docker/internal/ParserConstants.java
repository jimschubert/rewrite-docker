package com.github.jimschubert.rewrite.docker.internal;

public class ParserConstants {
    static final String DOUBLE_QUOTE = "\"";
    static final String SINGLE_QUOTE = "'";
    static final String TAB = "\t";
    static final String NEWLINE = "\n";
    static final String EQUAL = "=";
    static final String SPACE = " ";
    static final String EMPTY = "";
    static final String COMMA = ",";

    static final String SHELL = "SHELL";
    static final String ARG = "ARG";
    static final String FROM = "FROM";
    static final String MAINTAINER = "MAINTAINER";
    static final String RUN = "RUN";
    static final String CMD = "CMD";
    static final String ENTRYPOINT = "ENTRYPOINT";
    static final String ENV = "ENV";
    static final String ADD = "ADD";
    static final String COPY = "COPY";
    static final String VOLUME = "VOLUME";
    static final String EXPOSE = "EXPOSE";
    static final String USER = "USER";
    static final String WORKDIR = "WORKDIR";
    static final String LABEL = "LABEL";
    static final String STOPSIGNAL = "STOPSIGNAL";
    static final String HEALTHCHECK = "HEALTHCHECK";
    static final String ONBUILD = "ONBUILD";
    static final String COMMENT = "#";
}

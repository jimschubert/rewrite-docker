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
package com.github.jimschubert.rewrite.docker.internal;

public abstract class ParserConstants {
    private ParserConstants() {
    }

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

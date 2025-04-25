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

    public static final String SHELL = "SHELL";
    public static final String ARG = "ARG";
    public static final String FROM = "FROM";
    public static final String MAINTAINER = "MAINTAINER";
    public static final String RUN = "RUN";
    public static final String CMD = "CMD";
    public static final String ENTRYPOINT = "ENTRYPOINT";
    public static final String ENV = "ENV";
    public static final String ADD = "ADD";
    public static final String COPY = "COPY";
    public static final String VOLUME = "VOLUME";
    public static final String EXPOSE = "EXPOSE";
    public static final String USER = "USER";
    public static final String WORKDIR = "WORKDIR";
    public static final String LABEL = "LABEL";
    public static final String STOPSIGNAL = "STOPSIGNAL";
    public static final String HEALTHCHECK = "HEALTHCHECK";
    public static final String ONBUILD = "ONBUILD";
    public static final String COMMENT = "#";
}

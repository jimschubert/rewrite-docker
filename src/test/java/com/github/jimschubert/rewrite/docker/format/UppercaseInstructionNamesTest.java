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
package com.github.jimschubert.rewrite.docker.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;
import static org.junit.jupiter.api.Assertions.*;

class UppercaseInstructionNamesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UppercaseInstructionNames());
    }

    @Test
    void uppercaseFrom() {
        rewriteRun(
            //language=dockerfile
            dockerfile("from ubuntu:latest", "FROM ubuntu:latest")
        );
    }
    
    @Test
    void uppercaseRun() {
        rewriteRun(
            //language=dockerfile
            dockerfile("run echo hello world", "RUN echo hello world")
        );
    }
    
    @Test
    void uppercaseCmd() {
        rewriteRun(
            //language=dockerfile
            dockerfile("cmd echo hello world", "CMD echo hello world")
        );
    }
    
    @Test
    void uppercaseLabel() {
        rewriteRun(
            //language=dockerfile
            dockerfile("label version=1.0", "LABEL version=1.0")
        );
    }
    
    @Test
    void uppercaseWorkdir() {
        rewriteRun(
                //language=dockerfile
                dockerfile("workdir /app", "WORKDIR /app")
        );
    }

    @Test
    void uppercaseExpose() {
        rewriteRun(
                //language=dockerfile
                dockerfile("expose 8080", "EXPOSE 8080")
        );
    }

    @Test
    void uppercaseEnv() {
        rewriteRun(
                //language=dockerfile
                dockerfile("env KEY=value", "ENV KEY=value")
        );
    }

    @Test
    void uppercaseCopy() {
        rewriteRun(
                //language=dockerfile
                dockerfile("copy src dest", "COPY src dest")
        );
    }

    @Test
    void uppercaseAdd() {
        rewriteRun(
                //language=dockerfile
                dockerfile("add src dest", "ADD src dest")
        );
    }

    @Test
    void uppercaseEntrypoint() {
        rewriteRun(
                //language=dockerfile
                dockerfile("entrypoint echo hello", "ENTRYPOINT echo hello")
        );
    }


    @Test
    void uppercaseVolume() {
        rewriteRun(
                //language=dockerfile
                dockerfile("volume /data", "VOLUME /data")
        );
    }

    @Test
    void uppercaseStopsignal() {
        rewriteRun(
                //language=dockerfile
                dockerfile("stopsignal SIGTERM", "STOPSIGNAL SIGTERM")
        );
    }

    @Test
    void uppercaseShell() {
        rewriteRun(
                //language=dockerfile
                dockerfile("shell [\"/bin/bash\"]", "SHELL [\"/bin/bash\"]")
        );
    }

    @Test
    void uppercaseHealthcheck() {
        rewriteRun(
                //language=dockerfile
                dockerfile("healthcheck CMD curl -f http://localhost/", "HEALTHCHECK CMD curl -f http://localhost/")
        );
    }

    @Test
    void uppercaseArg() {
        rewriteRun(
                //language=dockerfile
                dockerfile("arg VERSION=latest", "ARG VERSION=latest")
        );
    }

    @Test
    void uppercaseOnbuild() {
        rewriteRun(
                //language=dockerfile
                dockerfile("onbuild RUN echo hello", "ONBUILD RUN echo hello")
        );
    }

}

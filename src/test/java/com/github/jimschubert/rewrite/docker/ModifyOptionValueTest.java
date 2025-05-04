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
package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModifyOptionValueTest implements RewriteTest {
    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.recipe(new ModifyOptionValue());
    }

    @Test
    void modifyOptionValueOrMatchValue() {
        rewriteRun(
                spec -> spec.recipe(new ModifyOptionValue("from", "build", "builder", null, false)),
            //language=dockerfile
            dockerfile(
                "COPY --from=build /myapp /usr/bin/",
                "COPY --from=builder /myapp /usr/bin/"
            )
        );
    }

    @Test
    void modifyOptionValueWithoutParentIncludeKeyDashes() {
        rewriteRun(
                spec -> spec.recipe(new ModifyOptionValue("--from", "build", "builder", null, false)),
                //language=dockerfile
                dockerfile(
                        "COPY --from=build /myapp /usr/bin/",
                        "COPY --from=builder /myapp /usr/bin/"
                )
        );
    }

    @Test
    void modifyOptionValueWrongParent() {
        rewriteRun(
                spec -> spec.recipe(new ModifyOptionValue("--from", "build", "builder", "ADD", false)),
                //language=dockerfile
                dockerfile(
                        "COPY --from=build /myapp /usr/bin/"
                )
        );
    }

    @Test
    void modifyOptionValueWithParentAndRegex() {
        rewriteRun(
                spec -> spec.recipe(new ModifyOptionValue(
                        "from",
                        "build",
                        "other",
                        "COPY.+/usr/bin/",
                        true)),
                //language=dockerfile
                dockerfile(
                        """
                            COPY --from=build /myapp /usr/bin/
                            COPY --from=builder /myapp /usr/local/bin/
                            """,
                        """
                            COPY --from=other /myapp /usr/bin/
                            COPY --from=builder /myapp /usr/local/bin/
                            """
                )
        );
    }

    @Test
    void modifyOptionValueMatchSingleOption() {
        rewriteRun(
                spec -> spec.recipe(new ModifyOptionValue(
                        "mount",
                        ".+/var/cache.+",
                        "type=tmpfs,destination=/tmp,size=300M",
                        "RUN",
                        false)),
                //language=dockerfile
                dockerfile(
                        """
                            FROM ubuntu
                            RUN rm -f /etc/apt/apt.conf.d/docker-clean; echo 'Binary::apt::APT::Keep-Downloaded-Packages "true";' > /etc/apt/apt.conf.d/keep-cache
                            RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
                                --mount=type=cache,target=/var/lib/apt,sharing=locked \
                                  apt update && apt-get --no-install-recommends install -y gcc
                            """,
                        """
                            FROM ubuntu
                            RUN rm -f /etc/apt/apt.conf.d/docker-clean; echo 'Binary::apt::APT::Keep-Downloaded-Packages "true";' > /etc/apt/apt.conf.d/keep-cache
                            RUN --mount=type=tmpfs,destination=/tmp,size=300M \
                                --mount=type=cache,target=/var/lib/apt,sharing=locked \
                                  apt update && apt-get --no-install-recommends install -y gcc
                            """
                )
        );
    }

    @Test
    void modifyOptionValueWithInvalidParent() {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> rewriteRun(
                spec -> spec.recipe(new ModifyOptionValue(
                        "from",
                        null,
                        "other",
                        "FROM",
                        false)),
                //language=dockerfile
                dockerfile(
                        """
                            COPY --from=build /myapp /usr/bin/
                            COPY --from=builder /myapp /usr/local/bin/
                            """
                )
        ));

        assertThat(assertionError).cause().isInstanceOf(RecipeRunException.class);
        RecipeRunException e = (RecipeRunException) assertionError.getCause();
        assertThat(e.getMessage())
                .contains("Invalid parent instruction: FROM");
    }
}

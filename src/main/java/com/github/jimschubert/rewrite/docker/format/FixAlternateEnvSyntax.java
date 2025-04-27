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

import com.github.jimschubert.rewrite.docker.DockerIsoVisitor;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Quoting;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class FixAlternateEnvSyntax extends Recipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Fix alternate ENV syntax";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Fix alternate ENV syntax by ensuring 'key=value' syntax is used instead of 'key value' syntax.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.Env visitEnv(Docker.Env env, ExecutionContext executionContext) {
                // note that we will not "undo" Docker's documented caveat.
                // ENV ONE TWO= THREE=world would be parsed by Docker as ONE="TWO= THREE=world".
                // This recipe will not change that behavior and will only add the equals and ensure
                // the resulting string is correct.
                List<DockerRightPadded<Docker.KeyArgs>> args = ListUtils.flatMap(
                        env.getArgs(),
                        keyArgs -> keyArgs.map(a -> {
                            a = a.withHasEquals(true);
                            if (a.getValue().getText().contains(" ") && a.getQuoting() != Quoting.DOUBLE_QUOTED) {
                                a = a.withQuoting(Quoting.DOUBLE_QUOTED);
                            }

                            return a;
                        })
                );
                env = env.withArgs(args);
                return super.visitEnv(env, executionContext);
            }
        };
    }
}

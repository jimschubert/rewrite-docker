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

import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Marker;

import java.util.Objects;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class NameAllStages extends Recipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Name all stages";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Name all stages in a Dockerfile (except the final stage).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.Document visitDocument(Docker.Document dockerfile, ExecutionContext ctx) {
                for (int i = dockerfile.getStages().size() - 1; i >= 0; i--) {
                    Docker.Stage stage = dockerfile.getStages().get(i);
                    stage = stage.withMarkers(stage.getMarkers().addIfAbsent(new Counter(UUID.randomUUID(), i, i == dockerfile.getStages().size() - 1)));
                    dockerfile.getStages().set(i, stage);
                }

                return super.visitDocument(dockerfile, ctx);
            }

            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext executionContext) {
                Docker.Stage stage = Objects.requireNonNull(getCursor().getParent()).firstEnclosing(Docker.Stage.class);
                if (stage != null && stage.getMarkers() != null && stage.getMarkers().findFirst(Counter.class).isPresent()) {
                    Counter counter = stage.getMarkers().findFirst(Counter.class).get();
                    if (!counter.isLast && (from.getAlias().getText() == null || from.getAlias().getText().isEmpty())) {
                        return from.alias("stage" + counter.index);
                    }
                }

                return from;
            }

            @Override
            public Docker.Copy visitCopy(Docker.Copy copy, ExecutionContext executionContext) {
                if (copy.getOptions() != null && copy.getOptions().stream().anyMatch(
                        o -> {
                            Docker.KeyArgs key = o.getElement().getKeyArgs();
                            return (key != null && "--from".equals(key.key())) && (key.value() != null && key.value().matches("^\\d+$"));
                        })) {
                    return copy.withOptions(
                            ListUtils.map(copy.getOptions(), o -> {
                                if (o == null || o.getElement() == null) {
                                    return o;
                                }

                                Docker.KeyArgs key = o.getElement().getKeyArgs();
                                if (key != null && "--from".equals(key.key()) && (key.value() != null && key.value().matches("^\\d+$"))) {
                                    int index = Integer.parseInt(key.value());
                                    return o.map(e -> e.withKeyArgs(key.withValue(key.getValue().withText("stage" + index))));
                                }

                                return o;
                            })
                    );
                }

                return copy;
            }
        };
    }

    @Value
    static class Counter implements Marker {
        @EqualsAndHashCode.Exclude
        @With
        UUID id;

        int index;
        boolean isLast;
    }
}

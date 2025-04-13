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
import lombok.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class RemoveImagePlatform extends Recipe {
    @Option(displayName = "A regular expression to locate an image entry.",
            description = "A regular expression to locate an image entry, defaults to `.+` (all images).",
            example = ".*",
            required = false)
    String matchImage;

    @Override
    public String getDisplayName() {
        return "Remove image platform";
    }

    @Override
    public String getDescription() {
        return "Remove the platform from an image on the FROM instruction.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(){
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext executionContext) {
                String emptyPlatform = "";
                if (".*".equals(matchImage) || ".+".equals(matchImage) || matchImage == null) {
                    from = from.platform(emptyPlatform);
                } else {
                    Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                    if (matcher.matches()) {
                        from = from.platform(emptyPlatform);
                    }
                }
                return super.visitFrom(from, executionContext);
            }
        };
    }
}

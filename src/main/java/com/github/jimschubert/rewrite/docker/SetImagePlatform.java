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
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class SetImagePlatform extends Recipe {
    @Option(displayName = "Platform",
            description = "The platform to set in the FROM instruction.",
            example = "linux/amd64")
    String platform;

    @Option(displayName = "A regular expression to locate an image entry.",
            description = "A regular expression to locate an image entry, defaults to `.+` (all images).",
            example = ".*",
            required = false)
    String matchImage;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Set the --platform flag in a FROM instruction";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Set the --platform flag in a FROM instruction.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext executionContext) {
                from = super.visitFrom(from, executionContext);
                if (matchImage == null || ".*".equals(matchImage) || ".+".equals(matchImage)) {
                    return from.platform(platform);
                }
                Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                if (matcher.matches()) {
                    return from.platform(platform);
                }

                return from;
            }
        };
    }
}

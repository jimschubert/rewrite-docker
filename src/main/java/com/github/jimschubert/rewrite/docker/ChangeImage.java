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
import org.jspecify.annotations.Nullable;
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
public class ChangeImage extends Recipe {

    @Option(displayName = "Match image",
            description = "A regular expression to locate an image entry.",
            example = ".*/ubuntu/.*")
    String matchImage;

    @Option(displayName = "New image",
            description = "The new image for the image found by `oldImage`. Can be format `registry/image`, `image`, `image:tag`, etc.",
            example = "alpine")
    String newImage;

    @Nullable
    @Option(displayName = "New version",
            description = "The new version (tag or digest) for the image found by `oldImage`. Can be format `:tagName`, `@digest`, or `tagName`. " +
                          "If not provided, the version will be left as-is. " +
                          "To unset a tag, newImage must not contain a tag, and newVersion must be set to an empty string.",
            example = ":latest",
            required = false)
    String newVersion;

    @Nullable
    @Option(displayName = "New platform",
            description = "The new platform for the image found by `matchImage`. Can be full format " +
                          "(`--platform=linux/amd64`), partial (`linux/amd64`)" +
                          "If not provided, the platform will be left as-is. " +
                          "To unset a platform, newPlatform must be set to an empty string.",
            example = "--platform=linux/amd64",
            required = false)
    String newPlatform;

    @Override
    public String getDisplayName() {
        return "Change a docker image name";
    }

    @Override
    public String getDescription() {
        return "Change a docker image name in a FROM instruction.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext executionContext) {
                if (matchImage == null || newImage == null) {
                    return from;
                }

                Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                if (matcher.matches()) {
                    String version = newVersion;
                    String image = null;
                    String platform = null;

                    // if newImage contains tag, we need to replace it via withTag, else with '@' digest we replace via withDigest
                    if (newImage.contains("@")) {
                        String[] parts = newImage.split("@");
                        image = parts[0];
                        if (parts.length > 1) {
                            version = parts[1];
                        }
                    } else if (newImage.contains(":")) {
                        String[] parts = newImage.split(":");
                        image = parts[0];
                        if (parts.length > 1) {
                            version = parts[1];
                        }
                    } else {
                        image = newImage;
                    }

                    if (newPlatform != null) {
                        platform = newPlatform;
                    } else {
                        platform = from.getPlatform().getElement().getText();
                    }

                    if (image != null && !image.equals(from.getImage().getElement().getText())) {
                        from = from.image(image);
                    }

                    if (version != null && !version.equals(from.getVersion().getElement().getText())) {
                        from = from.version(version);
                    }

                    if (platform != null && !platform.equals(from.getPlatform().getElement().getText())) {
                        from = from.platform(platform);
                    }

                    return super.visitFrom(from, executionContext);
                }

                return from;
            }
        };
    }
}

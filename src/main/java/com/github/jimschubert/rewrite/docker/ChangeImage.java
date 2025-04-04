package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Marker;

import java.util.UUID;
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
                Docker.From newFrom = super.visitFrom(from, executionContext);
                if (matchImage == null || newImage == null) {
                    return newFrom;
                }

                if (newFrom.getMarkers().findFirst(Modified.class).filter(m -> m.matchImage.equals(matchImage)
                        && m.newImage.equals(newImage)
                        && m.newPlatform.equals(newPlatform)
                        && m.newVersion.equals(newVersion)
                ).isPresent()) {
                    // already changed
                    return newFrom;
                }

                Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                if (matcher.matches()) {
                    Marker modifiedMarker = new Modified(
                            UUID.randomUUID(),
                            matchImage,
                            newImage,
                            newPlatform == null ? "" : newPlatform,
                            newVersion == null ? "" : newVersion);

                    String image = from.getImage().getElement().getText();
                    String version = from.getVersion().getElement().getText();
                    String platform = from.getPlatform().getElement().getText();

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

                    if (newVersion != null) {
                        version = newVersion;
                    }

                    if (newPlatform != null) {
                        platform = newPlatform;
                    } else {
                        platform = from.getPlatform().getElement().getText();
                    }

                    return newFrom.withImage(image)
                            .withVersion(version)
                            .withPlatform(platform)
                            .withMarkers(newFrom.getMarkers().add(modifiedMarker));
                }

                return newFrom;
            }
        };
    }

    @Value
    static class Modified implements Marker {
        @EqualsAndHashCode.Exclude
        @With
        UUID id;

        String matchImage;
        String newImage;
        String newPlatform;
        String newVersion;
    }
}

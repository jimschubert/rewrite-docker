package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Dockerfile;
import com.github.jimschubert.rewrite.docker.tree.Space;
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

    @Option(displayName = "Old image",
            description = "A regular expression to locate an image entry.",
            example = ".*/ubuntu/.*")
    String oldImage;

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
            public Dockerfile.From visitFrom(Dockerfile.From from, ExecutionContext executionContext) {
                Dockerfile.From newFrom = super.visitFrom(from, executionContext);
                if (oldImage == null || newImage == null) {
                    return newFrom;
                }

                if (newFrom.getMarkers().findFirst(Modified.class).filter(m -> m.oldImage.equals(oldImage) && m.newImage.equals(newImage)).isPresent()) {
                    // already changed
                    return newFrom;
                }

                Matcher matcher = Pattern.compile(oldImage).matcher(from.getImageSpecWithVersion());
                if (matcher.matches()) {
                    Marker modifiedMarker = new Modified(UUID.randomUUID(), oldImage, newImage);

                    // if newImage contains tag, we need to replace it via withTag, else with '@' digest we replace via withDigest
                    if (newImage.contains(":")) {
                        String[] parts = newImage.split(":");
                        String image = parts[0];
                        String tag = null;
                        if (parts.length > 1) {
                            tag = parts[1];
                        }

                        newFrom = newFrom.withImage(image)
                            .withTag(tag)
                            .withMarkers(newFrom.getMarkers().addIfAbsent(modifiedMarker));
                    } else if (newImage.contains("@")) {
                        String[] parts = newImage.split("@");
                        String image = parts[0];
                        String digest = null;
                        if (parts.length > 1) {
                            digest = parts[1];
                        }

                        newFrom = newFrom.withImage(image)
                            .withDigest(digest)
                            .withMarkers(newFrom.getMarkers().addIfAbsent(modifiedMarker));
                    } else if (newVersion == null) {
                        return newFrom.withImage(newImage)
                            .withMarkers(newFrom.getMarkers().addIfAbsent(modifiedMarker));
                    } else {
                        // remove version (no tag or digest specified)
                        return newFrom.withImage(newImage)
                            .withVersion(null)
                            .withMarkers(newFrom.getMarkers().addIfAbsent(modifiedMarker));
                    }
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

        String oldImage;
        String newImage;
    }
}

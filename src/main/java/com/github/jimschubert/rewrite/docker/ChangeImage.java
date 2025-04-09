package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
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
                if (from.getMarkers().findFirst(Modified.class).filter(m -> m.equals(new Modified(
                        UUID.randomUUID(), // excluded from equals
                        matchImage,
                        newImage,
                        newPlatform,
                        newVersion))
                ).isPresent()) {
                    // already changed
                    return from;
                }

                if (matchImage == null || newImage == null) {
                    return from;
                }

                Matcher matcher = Pattern.compile(matchImage).matcher(from.getImageSpecWithVersion());
                if (matcher.matches()) {
                    Marker modifiedMarker = new Modified(
                            UUID.randomUUID(),
                            matchImage,
                            newImage,
                            newPlatform,
                            newVersion);

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

                    boolean modified = false;
                    if (image != null && !image.equals(from.getImage().getElement().getText())) {
                        modified = true;
                        from = from.withImage(image);
                    }

                    if (version != null && !version.equals(from.getVersion().getElement().getText())) {
                        modified = true;
                        from = from.withVersion(version);
                    }

                    if (platform != null && !platform.equals(from.getPlatform().getElement().getText())) {
                        modified = true;
                        from = from.withPlatform(platform);
                    }

                    if (modified) {
                        from = from.withMarkers(from.getMarkers().add(modifiedMarker));
                    }

                    return super.visitFrom(from, executionContext);
                }

                return from;
            }
        };
    }

    // need a maker due to "helper" with* functions on non-final fields in the From class
    @Value
    private static class Modified implements Marker {
        @EqualsAndHashCode.Exclude
        @With
        UUID id;

        String matchImage;
        String newImage;
        String newPlatform;
        String newVersion;
    }
}

package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Dockerfile;
import lombok.*;
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
            description = "The new image for the image found by `oldImage`.",
            example = "alpine:latest")
    String newImage;

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
            public Dockerfile visitFrom(Dockerfile.From from, ExecutionContext executionContext) {
                Dockerfile.From newFrom = (Dockerfile.From) super.visitFrom(from, executionContext);
                if (oldImage == null || newImage == null) {
                    return newFrom;
                }

                if (newFrom.getMarkers().findFirst(Modified.class).filter(m -> m.oldImage.equals(oldImage) && m.newImage.equals(newImage)).isPresent()) {
                    // already changed
                    return newFrom;
                }

                Matcher matcher = Pattern.compile(oldImage).matcher(from.getImageSpec());
                if (matcher.matches()) {
                    return ((Dockerfile.From)newFrom.copyPaste()).withImageSpec(newImage)
                            .withMarkers(newFrom.getMarkers().addIfAbsent(new Modified(UUID.randomUUID(), oldImage, newImage)));
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

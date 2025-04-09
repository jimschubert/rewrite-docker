package com.github.jimschubert.rewrite.docker.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class DockerRightPadded<T> {
    @EqualsAndHashCode.Include(rank=1000)
    @With
    T element;

    @With
    Space after;

    @With
    Markers markers;

    public DockerRightPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public static <T> List<T> getElements(List<DockerRightPadded<T>> ls) {
        List<T> list = new ArrayList<>();
        for (DockerRightPadded<T> l : ls) {
            T elem = l.getElement();
            list.add(elem);
        }
        return list;
    }

    public static <P extends Docker> List<DockerRightPadded<P>> withElements(List<DockerRightPadded<P>> before, List<P> elements) {
        // a cheaper check for the most common case when there are no changes
        if (elements.size() == before.size()) {
            boolean hasChanges = false;
            for (int i = 0; i < before.size(); i++) {
                if (before.get(i).getElement() != elements.get(i)) {
                    hasChanges = true;
                    break;
                }
            }
            if (!hasChanges) {
                return before;
            }
        }

        List<DockerRightPadded<P>> after = new ArrayList<>(elements.size());
        Map<UUID, DockerRightPadded<P>> beforeById = before.stream().collect(Collectors
                .toMap(j -> j.getElement().getId(), Function.identity()));

        for (P t : elements) {
            if (beforeById.get(t.getId()) != null) {
                DockerRightPadded<P> found = beforeById.get(t.getId());
                after.add(found.withElement(t));
            } else {
                after.add(new DockerRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
            }
        }

        return after;
    }

    public static <T> DockerRightPadded<T> build(T element) {
        return new DockerRightPadded<>(element, Space.EMPTY, Markers.EMPTY);
    }

    @Nullable
    public static <T> DockerRightPadded<T> withElement(@Nullable DockerRightPadded<T> before, @Nullable T elements) {
        if (before == null) {
            if (elements == null) {
                return null;
            }
            return new DockerRightPadded<>(elements, Space.EMPTY, Markers.EMPTY);
        }
        if (elements == null) {
            return null;
        }
        return before.withElement(elements);
    }

    @Override
    public String toString() {
        return "DockerfileRightPadded(element=" + element.getClass().getSimpleName() + ", after=" + after + ')';
    }
}

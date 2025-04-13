package com.github.jimschubert.rewrite.docker.trait;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.openrewrite.Cursor;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.regex.Pattern;

@Getter
@AllArgsConstructor
public class DockerLiteral implements Trait<Docker.@NonNull Literal> {
    Cursor cursor;

    public String getText() {
        return getTree().getText();
    }

    public DockerLiteral withText(String newText) {
        Docker.Literal literal = getTree().withText(newText);
        cursor = new Cursor(cursor.getParent(), literal);
        return this;
    }

    public static class Matcher extends SimpleTraitMatcher<@NonNull DockerLiteral> {
        private final Pattern pattern;
        public Matcher(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        protected DockerLiteral test(@NonNull Cursor cursor) {
            if (pattern != null && cursor.getValue() instanceof Docker.Literal) {
                String text = ((Docker.Literal)cursor.getValue()).getText();
                if (text != null && pattern.matcher(text).matches()) {
                    return new DockerLiteral(cursor);
                }
            }
            return null;
        }
    }
}

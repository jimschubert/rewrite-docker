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

    public static Matcher matcher(Pattern pattern) {
        return new Matcher(pattern);
    }

    public static Matcher matcher(String pattern) {
        return new Matcher(Pattern.compile(pattern));
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

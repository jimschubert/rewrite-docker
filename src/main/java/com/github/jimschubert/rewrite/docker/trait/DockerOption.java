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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.Optional;

@Getter
@AllArgsConstructor
public class DockerOption implements Trait<Docker.@NonNull Option> {
    Cursor cursor;

    public Docker.KeyArgs getArgs() {
        return getTree().getKeyArgs();
    }

    public DockerOption withArgs(Docker.KeyArgs newArgs) {
        Docker.Option option = getTree().withKeyArgs(newArgs);
        cursor = new Cursor(cursor.getParent(), option);
        return this;
    }

    public static Matcher matcher(String key, @Nullable String value, boolean regexMatch) {
        return new Matcher(key, value, regexMatch);
    }

    public static class Matcher extends SimpleTraitMatcher<@NonNull DockerOption> {
        private final String key;
        private final String value;
        private final boolean regexMatch;

        public Matcher(String key, String value, boolean regexMatch) {
            this.key = key;
            this.value = value;
            this.regexMatch = regexMatch;
        }

        @Override
        protected DockerOption test(Cursor cursor) {
            if (cursor.getValue() instanceof Docker.Option) {
                Docker.Option option = cursor.getValue();
                Docker.KeyArgs args = option.getKeyArgs();
                Optional<String> key = Optional.ofNullable(args.key());
                Optional<String> value = Optional.ofNullable(args.value());
                if (this.regexMatch) {
                    if (key.isPresent() && !key.get().matches(this.key) && !key.get().replaceFirst("--", "").matches(this.key)) {
                        return null;
                    }
                    if (this.value != null && value.isPresent() && !value.get().matches(this.value)) {
                        return null;
                    }
                } else {
                    if (key.isPresent() && !key.get().equals(this.key)) {
                        return null;
                    }
                    if (value.isPresent() && !value.get().equals(this.value)) {
                        return null;
                    }
                }

                return new DockerOption(cursor);
            }

            return null;
        }
    }
}

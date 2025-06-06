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

import java.util.regex.Pattern;

public class Traits {
    public static DockerLiteral.Matcher literal(String pattern) {
        return new DockerLiteral.Matcher(pattern == null ? null : Pattern.compile(pattern));
    }

    public static DockerLiteral.Matcher literal(Pattern pattern) {
        return new DockerLiteral.Matcher(pattern);
    }

    public static DockerOption.Matcher option(String key, String value, boolean regexMatch) {
        return new DockerOption.Matcher(key, value, regexMatch);
    }
}

package com.github.jimschubert.rewrite.docker.trait;

import java.util.regex.Pattern;

public class Traits {
    public static DockerLiteral.Matcher literal(String pattern) {
        return new DockerLiteral.Matcher(pattern == null ? null : Pattern.compile(pattern));
    }

    public static DockerLiteral.Matcher literal(Pattern pattern) {
        return new DockerLiteral.Matcher(pattern);
    }
}

package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Space;

public record StringWithPadding(String content, Space prefix, Space suffix) { }
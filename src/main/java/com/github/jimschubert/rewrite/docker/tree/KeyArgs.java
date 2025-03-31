package com.github.jimschubert.rewrite.docker.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class KeyArgs {
    @EqualsAndHashCode.Include
    String key;
    @EqualsAndHashCode.Include
    String value;
    boolean hasEquals;
    Quoting quoting;
}

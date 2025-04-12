package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.tree.Space;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class ParserState {
    private Space prefix = Space.EMPTY;
    private Space rightPadding = Space.EMPTY;
    private char escapeChar = '\\';
    private boolean isContinuation = false;

    public void reset() {
        prefix = Space.EMPTY;
        rightPadding = Space.EMPTY;
        escapeChar = '\\';
    }

    String getEscapeString() {
        return String.valueOf(escapeChar);
    }

    void appendPrefix(Space prefix) {
        if (prefix != null) {
            this.prefix = prefix.withWhitespace(this.prefix.getWhitespace() + prefix.getWhitespace());
        }
    }

    void resetPrefix() {
        prefix = Space.EMPTY;
    }

    ParserState copy() {
        ParserState copy = new ParserState();
        copy.prefix = this.prefix;
        copy.rightPadding = this.rightPadding;
        copy.escapeChar = this.escapeChar;
        copy.isContinuation = this.isContinuation;
        return copy;
    }
}

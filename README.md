# rewrite-docker

This is a pet project to support dockerfiles in OpenRewrite.

It does _not_ work well enough for you to use it. It's a work in progress.

TODOs:

* Implement a more solid lexer/parser to retain whitespace.
* Update all print methods (aside from FROM) to properly print prefix + right-padding
* Update all types with Right-padded literals to have simplified with* methods
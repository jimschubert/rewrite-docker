package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class ChangeImageTest implements RewriteTest {
    @Test
    void changeImageName(){
        rewriteRun(
            // TODO: determine why stage iteration results in two cycles
            spec -> spec.expectedCyclesThatMakeChanges(2)
                    .recipe(new ChangeImage("old.*", "newImage")),
            dockerfile(
                """
                FROM oldImage
                """
                ,
                """
                FROM newImage
                """
            )
        );
    }
}
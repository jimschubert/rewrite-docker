package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class ChangeImageTest implements RewriteTest {
    @Test
    void changeImageName() {
        rewriteRun(
                // TODO: determine why stage iteration results in two cycles
                spec -> spec.expectedCyclesThatMakeChanges(2)
                        .recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM oldImage
                    """,
                    """
                    FROM newImage
                    """
                )
        );
    }

    @Test
    void changeImageNameWithOtherElements() {
        rewriteRun(
                // TODO: determine why stage iteration results in two cycles
                spec -> spec.expectedCyclesThatMakeChanges(2)
                        .recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM --platform=linux/amd64 oldImage AS base
                    FROM --platform=linux/amd64 doNotTouch
                    """,
                    """
                    FROM --platform=linux/amd64 newImage AS base
                    FROM --platform=linux/amd64 doNotTouch
                    """
                )
        );
    }


    @Test
    void changeImageNameWithOtherElementsLowercaseAs() {
        rewriteRun(
                // TODO: determine why stage iteration results in two cycles
                spec -> spec.expectedCyclesThatMakeChanges(2)
                        .recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM --platform=linux/amd64 oldImage as base
                    FROM --platform=linux/amd64 doNotTouch
                    """,
                    """
                    FROM --platform=linux/amd64 newImage as base
                    FROM --platform=linux/amd64 doNotTouch
                    """
                )
        );
    }


    @Test
    void keepVersionWhenSuppliedNull() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(2)
                        .recipe(new ChangeImage("oldImage:.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM oldImage:latest
                    """,
                    """
                    FROM newImage:latest
                    """
                )
        );
    }

    @Test
    void removeVersionWhenSuppliedEmptyString() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(2)
                        .recipe(new ChangeImage("oldImage:.*", "newImage", "", null)),
                dockerfile(
                    """
                    FROM oldImage:latest
                    """
                    ,
                    """
                    FROM newImage
                    """
                )
        );
    }

    @Test
    void removePlatformWhenSuppliedEmptyString() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(2)
                        .recipe(new ChangeImage("oldImage:.*", "newImage", null, "")),
                dockerfile(
                    """
                    FROM --platform=linux/amd64 oldImage:latest
                    """,
                    """
                    FROM newImage:latest
                    """
                )
        );
    }

    @Test
    void testChangeImageWithNonStandardWhitespace() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(2)
                        .recipe(new ChangeImage("old.*", "newImage", null, null)),
                dockerfile(
                    """
                    FROM    --platform=linux/amd64    oldImage:latest as   base  
                    """,
                    """
                    FROM    --platform=linux/amd64    newImage:latest as   base  
                    """
                )
        );
    }
}
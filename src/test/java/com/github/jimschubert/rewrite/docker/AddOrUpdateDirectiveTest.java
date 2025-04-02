package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class AddOrUpdateDirectiveTest  implements RewriteTest {
    @Test
    void addDirective() {
        rewriteRun(
            spec -> spec.expectedCyclesThatMakeChanges(1)
                    .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                    .recipe(new AddOrUpdateDirective("check=error=true")),
            dockerfile(
                """
                FROM myImage:latest
                """
                ,
                """
                # check=error=true
                FROM myImage:latest
                """
            )
        );
    }

    @Test
    void addDirectiveWhenMultiple() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("syntax=docker/dockerfile:1")),
                dockerfile(
                        """
                        # check=error=true
                        # escape=`
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # check=error=true
                        # escape=`
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirective() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("check=error=true")),
                dockerfile(
                        """
                        # check=error=false
                        FROM myImage:latest
                        """
                        ,
                        """
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirectiveComplex() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("check=skip=JSONArgsRecommended;error=true")),
                dockerfile(
                        """
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # check=skip=JSONArgsRecommended;error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirectiveWhenMultiple() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("escape=|")),
                dockerfile(
                        """
                        # syntax=docker/dockerfile:1
                        # escape=`
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # escape=|
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void updateDirectiveWhenCasingMismatch() {
        // According to docker's docs, all of these are the same. But the parser currently doesn't return the preceding spaces or spaces around the equals sign.
        // #directive=value
        // # directive =value
        // #	directive= value
        // # directive = value
        // #	  dIrEcTiVe=value
        // see https://docs.docker.com/reference/dockerfile/#syntax
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("escape=|")),
                dockerfile(
                        """
                        # syntax=docker/dockerfile:1
                        # eScApE=`
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # escape=|
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }

    @Test
    void addDirectiveWhenMixedWitHComments() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1)
                        .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
                        .recipe(new AddOrUpdateDirective("escape=|")),
                dockerfile(
                        """
                        # syntax=docker/dockerfile:1
                        # unknowndirective=value
                        # escape=`
                        # check=error=true
                        FROM myImage:latest
                        """
                        ,
                        """
                        # syntax=docker/dockerfile:1
                        # unknowndirective=value
                        # escape=|
                        # check=error=true
                        FROM myImage:latest
                        """
                )
        );
    }
}
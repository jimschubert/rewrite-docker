package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class ModifyLiteralTest implements RewriteTest {
    @Test
    void replacesAllMatchingLiterals() {
        rewriteRun(
                spec -> spec.recipe(new ModifyLiteral("(?:.*)(17)(?:(?=\\-jdk-slim|-openjdk).*)", "21")),

                dockerfile(
                        """
                        # Use a specific base image with Java installed
                        FROM openjdk:17-jdk-slim
                        
                        # Set environment variables for Java paths
                        ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
                        ENV PATH=$JAVA_HOME/bin:$PATH
                        
                        # Example RUN commands using hard-coded Java paths
                        RUN /usr/lib/jvm/java-17-openjdk-amd64/bin/java -version
                        RUN /usr/lib/jvm/java-17-openjdk-amd64/bin/javac -version
                        
                        # Set the working directory
                        WORKDIR /app
                        
                        # Copy application files into the container
                        COPY . .
                        
                        # Compile the application
                        RUN /usr/lib/jvm/java-17-openjdk-amd64/bin/javac Main.java
                        
                        # Command to run the application
                        CMD ["/usr/lib/jvm/java-17-openjdk-amd64/bin/java", "Main"]
                        """,
                        """
                        # Use a specific base image with Java installed
                        FROM openjdk:21-jdk-slim
                        
                        # Set environment variables for Java paths
                        ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
                        ENV PATH=$JAVA_HOME/bin:$PATH
                        
                        # Example RUN commands using hard-coded Java paths
                        RUN /usr/lib/jvm/java-21-openjdk-amd64/bin/java -version
                        RUN /usr/lib/jvm/java-21-openjdk-amd64/bin/javac -version
                        
                        # Set the working directory
                        WORKDIR /app
                        
                        # Copy application files into the container
                        COPY . .
                        
                        # Compile the application
                        RUN /usr/lib/jvm/java-21-openjdk-amd64/bin/javac Main.java
                        
                        # Command to run the application
                        CMD ["/usr/lib/jvm/java-21-openjdk-amd64/bin/java", "Main"]"""
                )
        );
    }
}

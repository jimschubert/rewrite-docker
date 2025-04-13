package com.github.jimschubert.rewrite.docker.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Pattern;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;
import static com.github.jimschubert.rewrite.docker.trait.Traits.literal;

class DockerLiteralTest implements RewriteTest {
    @Test
    void matchAllLiterals() {
        // TODO: treat key and value in KeyArgs as literals
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              literal(Pattern.compile(".*(?<=java-)(17)-.*|.*(17)(?=-jdk-slim).*")).asVisitor(lit -> lit.withText(
                lit.getText().replace("17", "21")
              ).getTree())
            )
          ),
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
                  ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
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
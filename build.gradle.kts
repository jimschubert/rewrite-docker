plugins {
    id("java")
    id("com.diffplug.spotless") version "7.0.3"
}

group = "com.github.jimschubert"
version = project.findProperty("version") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

spotless {
    java {
        target("src/**/*.java")
        removeUnusedImports()
        endWithNewline()
        licenseHeader("""
            /*
             * Copyright (c) ${'$'}today.year Jim Schubert
             * Licensed under the Apache License, Version 2.0 (the "License");
             * you may not use this file except in compliance with the License.
             * You may obtain a copy of the License at
             *
             *     http://www.apache.org/licenses/LICENSE-2.0
             *
             * Unless required by applicable law or agreed to in writing, software
             * distributed under the License is distributed on an "AS IS" BASIS,
             * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
             * See the License for the specific language governing permissions and
             * limitations under the License.
             */
        """.trimIndent())

        targetExclude("**/generated/**")

    }

    format("misc") {
        target(
            "**/*.gradle",
            "**/*.md",
            "**/*.properties",
            "**/*.yml",
            "**/*.yaml"
        )
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }

    encoding("UTF-8")
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(11)
}

tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(17)
}

dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:latest.release"))

    // lombok is optional, but recommended for authoring recipes
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation("org.jetbrains:annotations:23.0.0")
    implementation("org.openrewrite:rewrite-test:latest.release")

    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

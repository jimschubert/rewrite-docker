plugins {
    id("java")
}

group = "com.github.jimschubert"
version = project.findProperty("version") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))

    // lombok is optional, but recommended for authoring recipes
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation("org.jetbrains:annotations:23.0.0")
    implementation("org.openrewrite:rewrite-test:latest.release")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

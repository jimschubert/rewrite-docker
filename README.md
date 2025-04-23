# Rewrite-Docker

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17-blue.svg)](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
[![Build](https://github.com/jimschubert/rewrite-docker/actions/workflows/build.yml/badge.svg)](https://github.com/jimschubert/rewrite-docker/actions/workflows/build.yml)

A library that extends [OpenRewrite](https://github.com/openrewrite/rewrite) to provide Dockerfile parsing, analysis, and transformation capabilities.

## Project Status

⚠️ **Work in Progress** ⚠️

This is a pet project currently under development. It does **not** work well enough for production use yet. There is no guarantee that it will be completed or maintained long-term.

## Overview

Rewrite-Docker aims to bring the power of OpenRewrite's AST-based transformation to Dockerfiles. This enables programmatic analysis and refactoring of Docker configuration files.

## Features

* Dockerfile AST (Abstract Syntax Tree) parsing
* Tree representation of Dockerfile instructions
* Visitor pattern for navigating and transforming Dockerfiles

## Getting Started

### Prerequisites

* Java 17 or higher
* Gradle

### Installation

The library is available via JitPack and can be added to your project as a dependency.

#### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.jimschubert:rewrite-docker:1.0.4'
}
```

#### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.jimschubert</groupId>
    <artifactId>rewrite-docker</artifactId>
    <version>1.0.4</version>
</dependency>
```

## Building

To build the project, you can use Gradle. Make sure you have Java 17 or higher installed.

```bash
./gradlew :spotlessApply build
```

## Usage Examples
_Coming soon_

## Development Roadmap
- [ ] Add parser error handling for invalid syntaxes
- [ ] Develop common Dockerfile recipes
- [ ] Add documentation and usage examples

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

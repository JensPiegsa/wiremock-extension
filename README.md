# wiremock-extension

[![codecov](https://codecov.io/gh/JensPiegsa/wiremock-extension/branch/master/graph/badge.svg)](https://codecov.io/gh/JensPiegsa/wiremock-extension)

The wiremock-extension is a [JUnit 5](https://junit.org/junit5/) [extension](https://junit.org/junit5/docs/current/user-guide/#extensions) that starts [WireMock](http://wiremock.org/) before running tests and stops it afterwards. It is similar to the [WireMockRule](https://github.com/tomakehurst/wiremock/blob/master/src/main/java/com/github/tomakehurst/wiremock/junit/WireMockRule.java) ([docs](http://wiremock.org/docs/junit-rule/)) for JUnit 4.

## Disclaimer

This extension is in an early alpha state. API might change at any time.

## Usage

### Step 1. Add the JitPack repository to your **pom.xml** file

    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

### Step 2. Add the wiremock-extension dependency

    <dependency>
        <groupId>com.github.JensPiegsa</groupId>
        <artifactId>wiremock-extension</artifactId>
        <version>0.1.2</version>
    </dependency>

### Step 3. Annotate test classes by `@ExtendsWith(MockitoExtension.class)`

* see [`ExampleTest`](https://github.com/JensPiegsa/wiremock-extension/blob/master/src/test/java/com/github/jenspiegsa/mockitoextension/ExampleTest.java) for further configuration and different use cases.

## Contribute

Feedback is welcome. The source is available on [Github](https://github.com/JensPiegsa/wiremock-extension/). Please [report any issues](https://github.com/JensPiegsa/wiremock-extension/issues).

## About

Plugin originally created by [Jens Piegsa](https://github.com/JensPiegsa).

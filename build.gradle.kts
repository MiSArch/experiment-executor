plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.misarch"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    //implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.115.Final:osx-aarch_64")
    //implementation("com.expediagroup:graphql-kotlin-spring-server:7.0.2")
    //implementation("com.expediagroup:graphql-kotlin-spring-client:7.0.2")
    //implementation("com.querydsl:querydsl-core")
    //implementation("com.graphql-java:graphql-java-extended-scalars:21.0")
    implementation("io.dapr:dapr-sdk-springboot:1.11.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.5")
    //runtimeOnly("org.postgresql:postgresql")
    //runtimeOnly("org.postgresql:r2dbc-postgresql")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

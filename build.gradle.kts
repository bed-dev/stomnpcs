plugins {
    kotlin("jvm") version "2.3.10"
}

group = "codes.bed.minestom"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    api("net.minestom:minestom:2026.03.03-1.21.11")

    testImplementation("org.tinylog:tinylog-api-kotlin:2.8.0-M1")
    testImplementation("org.tinylog:tinylog-impl:2.8.0-M1")
    testImplementation("org.tinylog:slf4j-tinylog:2.8.0-M1")
    testImplementation("io.github.revxrsal:lamp.common:4.0.0-rc.16")
    testImplementation("io.github.revxrsal:lamp.minestom:4.0.0-rc.16")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>("compileTestKotlin") {
    compilerOptions {
        javaParameters.set(true)
    }
}

tasks.register<JavaExec>("runTestServer") {
    group = "application"
    description = "Runs test server"

    dependsOn(tasks.named("testClasses"))

    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("codes.bed.minestom.npc.test.TestServerKt")
    standardInput = System.`in`
}

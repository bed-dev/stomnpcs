plugins {
    kotlin("jvm") version "2.3.10"
    // Add the Vanniktech plugin which supports the new Central Portal API
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "codes.bed.minestom"
version = "0.1.3"
description = "A small Minestom NPC library providing per-player dialogue holograms and configurable name displays"

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

// The new unified publishing block
mavenPublishing {
    coordinates(project.group.toString(), "npc", project.version.toString())

    pom {
        name.set("npc")
        description.set(project.description)
        inceptionYear.set("2024")
        url.set("https://github.com/bed-dev/stomnpcs")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("bed-dev")
                name.set("bed-dev")
                url.set("https://github.com/bed-dev")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/bed-dev/stomnpcs.git")
            developerConnection.set("scm:git:ssh://github.com/bed-dev/stomnpcs.git")
            url.set("https://github.com/bed-dev/stomnpcs")
        }
    }

    // 1. Point to the new Central Portal API
    // 2. automaticRelease = true tells Sonatype to publish immediately if validation passes
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    // Enable GPG signing using the keys we will provide via GitHub Actions
    signAllPublications()
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests.set(false)
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
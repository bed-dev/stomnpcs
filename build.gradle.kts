plugins {
    kotlin("jvm") version "2.3.10"
    id("maven-publish")
    id("signing")
}

group = "codes.bed.minestom"
version = "0.1.0"
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

// Publishing configuration for Maven Central (OSSRH)
// Safely read from Environment Variables (GitHub Actions) or fallback to properties (Local Builds)
val ossrhUsername = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrhUsername") as String?
val ossrhPassword = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrhPassword") as String?
val signingKey = System.getenv("GPG_PRIVATE_KEY") ?: project.findProperty("signingKey") as String?
val signingPassword = System.getenv("GPG_PASSPHRASE") ?: project.findProperty("signingPassword") as String?
val signingKeyRingFile: String? by project

java {
    // Produce sources and javadoc jars for publishing
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<org.gradle.api.publish.maven.MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("stomnpcs")
                description.set(project.description)
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
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            // NOTE: If you just created your Sonatype account recently at central.sonatype.com,
            // this legacy s01 URL will reject your upload.
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

signing {
    // Support either in-memory PGP key (signingKey) or a keyring file path (signingKeyRingFile)
    when {
        !signingKey.isNullOrBlank() -> useInMemoryPgpKeys(signingKey, signingPassword)
        !signingKeyRingFile.isNullOrBlank() -> useInMemoryPgpKeys(file(signingKeyRingFile!!).readText(), signingPassword)
        else -> {
            // No key configured; signing will fail if required by publishing. CI should provide keys.
        }
    }
    // Only sign if the publication exists
    sign(publishing.publications["mavenJava"])
}

tasks.test {
    useJUnitPlatform()
    // Don't fail the build when no tests are discovered (some test setups run a custom test server)
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
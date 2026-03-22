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

}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}
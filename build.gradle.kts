plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
}

group = "com.duckblade"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://repo.runelite.net")
    mavenCentral()
}

val ktorVersion = "3.1.0"
dependencies {
    compileOnly("net.runelite:client:latest.release")

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.17")
    implementation("io.ktor:ktor-client-okhttp-jvm:3.1.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
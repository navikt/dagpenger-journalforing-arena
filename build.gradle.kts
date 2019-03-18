import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("application")
    kotlin("jvm") version "1.3.10"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("info.solidsoft.pitest") version "1.3.0"
}

apply {
    plugin("com.diffplug.gradle.spotless")
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://dl.bintray.com/kittinunf/maven")
}

application {
    applicationName = "dagpenger-journalforing-arena"
    mainClassName = "no.nav.dagpenger.journalføring.arena.JournalføringArena"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val kotlinLoggingVersion = "1.6.22"
val kafkaVersion = "2.0.1"
val confluentVersion = "4.1.2"
val ktorVersion = "1.0.0"
val prometheusVersion = "0.6.0"
val fuelVersion = "1.15.0"
val log4j2Version = "2.11.1"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("no.nav.dagpenger:streams:0.3.0-SNAPSHOT")
    implementation("no.nav.dagpenger:events:0.3.1-SNAPSHOT")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-gson:$fuelVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    api("org.apache.kafka:kafka-clients:$kafkaVersion")
    api("org.apache.kafka:kafka-streams:$kafkaVersion")
    api("io.confluent:kafka-streams-avro-serde:$confluentVersion")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("com.vlkan.log4j2:log4j2-logstash-layout-fatjar:0.15")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-all:2.+")
    testImplementation("com.github.tomakehurst:wiremock:2.19.0")
    testImplementation("no.nav:kafka-embedded-env:2.0.2")
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint()
    }
}

pitest {
    threads = 4
    pitestVersion = "1.4.3"
    coverageThreshold = 80
    avoidCallsTo = setOf("kotlin.jvm.internal")
    targetClasses = setOf("no.nav.dagpenger.*")
}

tasks.getByName("test").finalizedBy("pitest")

tasks.withType<Test> {
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

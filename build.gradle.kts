import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

buildscript {
    repositories {
        jcenter()
    }
}

apply {
    plugin(Spotless.spotless)
}

repositories {
    mavenCentral()
    jcenter()
    maven("http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

application {
    applicationName = "dagpenger-journalforing-arena"
    mainClassName = "no.nav.dagpenger.journalføring.arena.JournalføringArenaKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.navikt:dagpenger-streams:2019.10.18-12.06.fbbb66cd150b")
    implementation(Dagpenger.Events)

    implementation(Dagpenger.Biblioteker.stsKlient)

    implementation(Prometheus.common)
    implementation(Prometheus.log4j2)

    implementation(Konfig.konfig)

    implementation(Fuel.fuel)
    implementation(Fuel.library("moshi"))
    implementation(Moshi.moshi)
    implementation(Moshi.moshiKotlin)
    implementation(Moshi.moshiAdapters)

    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.Logstash.logstashLayout)
    implementation(Kotlin.Logging.kotlinLogging)

    implementation(Kafka.clients)
    implementation(Kafka.streams)
    implementation(Kafka.Confluent.avroStreamSerdes)

    implementation(Ktor.serverNetty)

    testImplementation(kotlin("test"))
    testImplementation(Junit5.api)
    testImplementation(Junit5.kotlinRunner)
    testRuntimeOnly(Junit5.engine)
    testImplementation(Wiremock.standalone)
    testImplementation(KafkaEmbedded.env)
    testImplementation(Kafka.streamTestUtils)
    testImplementation(Mockk.mockk)
}

spotless {
    kotlin {
        ktlint(Klint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint(Klint.version)
    }
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

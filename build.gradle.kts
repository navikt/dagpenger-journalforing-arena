import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
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

val cxfVersion = "3.3.4"
val tjenestespesifikasjonerVersion = "1.2019.09.25-00.21-49b69f0625e0"

fun tjenestespesifikasjon(name: String) = "no.nav.tjenestespesifikasjoner:$name:$tjenestespesifikasjonerVersion"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(Dagpenger.Streams)
    implementation(Dagpenger.Events)

    implementation("no.finn.unleash:unleash-client-java:3.2.9")

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

    implementation(Ulid.ulid)

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

    // Soap stuff
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("com.sun.xml.ws:jaxws-tools:2.3.0.2")

    implementation(tjenestespesifikasjon("behandleArbeidOgAktivitetOppgave-v1-tjenestespesifikasjon"))
    implementation(tjenestespesifikasjon("ytelseskontrakt-v3-tjenestespesifikasjon"))

    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-policy:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("javax.activation:activation:1.1.1")
    implementation("no.nav.helse:cxf-prometheus-metrics:dd7d125")
    testImplementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    // Soap stuff end
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

tasks.withType<ShadowJar> {
    mergeServiceFiles()

    // Make sure the cxf service files are handled correctly so that the SOAP services work.
    // Ref https://stackoverflow.com/questions/45005287/serviceconstructionexception-when-creating-a-cxf-web-service-client-scalajava
    transform(ServiceFileTransformer::class.java) {
        setPath("META-INF/cxf")
        include("bus-extensions.txt")
    }
}

tasks.named("shadowJar") {
    dependsOn("test")
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

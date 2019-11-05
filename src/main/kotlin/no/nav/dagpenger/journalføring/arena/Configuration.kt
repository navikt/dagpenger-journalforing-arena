package no.nav.dagpenger.journalføring.arena

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.booleanType
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.PacketDeserializer
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topic
import org.apache.kafka.common.serialization.Serdes
import java.io.File

private val defaultProperties = ConfigurationMap(
    mapOf(
        "application.httpPort" to 8080.toString(),
        "allow.insecure.soap.requests" to false.toString()
    )
)

private val localProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "localhost:9092",
        "application.profile" to Profile.LOCAL.toString(),
        "srvdagpenger.journalforing.arena.username" to "user",
        "srvdagpenger.journalforing.arena.password" to "password",
        "securitytokenservice.url" to "https://localhost/SecurityTokenServiceProvider/",
        "behandlearbeidsytelsesak.v1.url" to "http://localhost/ail_ws/BehandleArbeidsytelseSak_v1"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "application.profile" to Profile.DEV.toString(),
        "securitytokenservice.url" to "https://sts-q2.test.local/SecurityTokenServiceProvider/",
        "behandlearbeidsytelsesak.v1.url" to "https://arena-q2.adeo.no/ail_ws/BehandleArbeidsytelseSak_v1"
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
        "application.profile" to Profile.PROD.toString(),
        "securitytokenservice.url" to "https://sts.adeo.no/SecurityTokenServiceProvider//",
        "behandlearbeidsytelsesak.v1.url" to "https://arena.adeo.no/ail_ws/BehandleArbeidsytelseSak_v1"
    )
)

private val defaultConfiguration =
    ConfigurationProperties.fromOptionalFile(File("/var/run/secrets/nais.io/vault/config.properties")) overriding ConfigurationProperties.systemProperties() overriding EnvironmentVariables

fun config(): Configuration {
    return when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-fss" -> defaultConfiguration overriding devProperties overriding defaultProperties
        "prod-fss" -> defaultConfiguration overriding prodProperties overriding defaultProperties
        else -> {
            defaultConfiguration overriding localProperties overriding defaultProperties
        }
    }
}

data class Configuration(
    val kafka: Kafka = Kafka(),
    val application: Application = Application(),
    val soapSTSClient: SoapSTSClient = SoapSTSClient(),
    val behandleArbeidsytelseSak: BehandleArbeidsytelseSak = BehandleArbeidsytelseSak()
) {
    data class Kafka(
        val dagpengerJournalpostTopic: Topic<String, Packet> = Topic(
            "privat-dagpenger-journalpost-mottatt-v1",
            keySerde = Serdes.String(),
            valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
        ),
        val user: String = config()[Key("srvdagpenger.journalforing.arena.username", stringType)],
        val password: String = config()[Key("srvdagpenger.journalforing.arena.password", stringType)],
        val brokers: String = config()[Key("kafka.bootstrap.servers", stringType)]
    ) {
        fun credential(): KafkaCredential? {
            return KafkaCredential(user, password)
        }
    }

    data class SoapSTSClient(
        val endpoint: String = config()[Key("securitytokenservice.url", stringType)],
        val username: String = config()[Key("srvdagpenger.journalforing.arena.username", stringType)],
        val password: String = config()[Key("srvdagpenger.journalforing.arena.username", stringType)],
        val allowInsecureSoapRequests: Boolean = config()[Key("allow.insecure.soap.requests", booleanType)]
    )

    data class BehandleArbeidsytelseSak(
        val endpoint: String = config()[Key("behandlearbeidsytelsesak.v1.url", stringType)]
    )

    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val user: String = config()[Key("srvdagpenger.journalforing.arena.username", stringType)],
        val password: String = config()[Key("srvdagpenger.journalforing.arena.password", stringType)],
        val httpPort: Int = config()[Key("application.httpPort", intType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}

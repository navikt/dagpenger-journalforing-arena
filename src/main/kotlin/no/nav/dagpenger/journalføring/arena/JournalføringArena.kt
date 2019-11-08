package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.soap.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.dagpenger.journalføring.arena.adapter.soap.SoapPort
import no.nav.dagpenger.journalføring.arena.adapter.soap.arena.SoapArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.soap.configureFor
import no.nav.dagpenger.journalføring.arena.adapter.soap.stsClient
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.util.Properties

private val logger = KotlinLogging.logger {}

internal object PacketKeys {
    const val NY_SØKNAD: String = "nySøknad"
    const val JOURNALPOST_ID: String = "journalpostId"
    const val AKTØR_ID: String = "aktørId"
    const val BEHANDLENDE_ENHETER: String = "behandlendeEnheter"
    const val NATURLIG_IDENT: String = "naturligIdent"
    const val ARENA_SAK_ID: String = "arenaSakId"
}

class JournalføringArena(private val configuration: Configuration, val arenaClient: ArenaClient) :
    River(configuration.kafka.dagpengerJournalpostTopic) {

    override val SERVICE_APP_ID = "dp-journalforing-arena"
    override val HTTP_PORT: Int = configuration.application.httpPort

    private val unleash: Unleash = DefaultUnleash(configuration.unleashConfig)

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(PacketKeys.ARENA_SAK_ID) }
        )
    }

    override fun onPacket(packet: Packet): Packet {

        val naturligIdent: String = packet.getStringValue(PacketKeys.NATURLIG_IDENT)
        val enhetId =
            packet.getObjectValue(PacketKeys.BEHANDLENDE_ENHETER) { behandlendeenhetAdapter.fromJsonValue(it)!! }
                .first().enhetId

        packet.putValue(
            PacketKeys.ARENA_SAK_ID,
            "1234"
        )
        try {
            if (unleash.isEnabled("dp-arena.HentSaker${configuration.application.profile.name}")) {
                val saker = arenaClient.hentArenaSaker(naturligIdent)
                saker.forEach {
                    logger.info { "Tilhører sak: ${it.saksId}" }
                }

                if (saker.isEmpty()) {
                    logger.info { "Har ingen saker" }
                }
            }
        } catch (exception: Exception) {
            logger.error(exception) { "Failed to get arena-saker" }
        }

        return packet
    }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = configuration.kafka.brokers,
            credential = configuration.kafka.credential()
        )
    }
}

fun main(args: Array<String>) {
    val configuration = Configuration()

    val arbeidOgAktivitet = SoapPort.arenaArbeidOgAktivitet(configuration.arenaArbeidOgAktivitet.endpoint)

    val behandleArbeidsytelseSak =
        SoapPort.behandleArbeidOgAktivitetOppgaveV1(configuration.behandleArbeidsytelseSak.endpoint)

    // val arenaSakVedtakService: SakVedtakService =
    //     SoapPort.sakVedtakService(configuration.arenaSakVedtakService.endpoint)

    val arenaClient: ArenaClient =
        SoapArenaClient(behandleArbeidsytelseSak, arbeidOgAktivitet)

    val soapStsClient = stsClient(
        stsUrl = configuration.soapSTSClient.endpoint,
        credentials = configuration.soapSTSClient.username to configuration.soapSTSClient.password
    )
    if (configuration.soapSTSClient.allowInsecureSoapRequests) {
        soapStsClient.configureFor(behandleArbeidsytelseSak, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
        // soapStsClient.configureFor(arenaSakVedtakService, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
        soapStsClient.configureFor(arbeidOgAktivitet, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
    } else {
        soapStsClient.configureFor(behandleArbeidsytelseSak)
        // soapStsClient.configureFor(arenaSakVedtakService)
        soapStsClient.configureFor(arbeidOgAktivitet)
    }

    val service = JournalføringArena(configuration, arenaClient)

    service.start()
}

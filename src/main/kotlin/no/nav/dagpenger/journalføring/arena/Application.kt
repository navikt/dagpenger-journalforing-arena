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
import no.nav.dagpenger.streams.HealthCheck
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Predicate
import java.util.Properties

private val logger = KotlinLogging.logger {}

internal class Application(
    private val configuration: Configuration,
    private val journalføringArena: JournalføringArena
) :
    River(configuration.kafka.dagpengerJournalpostTopic) {

    override val SERVICE_APP_ID = "dp-journalforing-arena"
    override val HTTP_PORT: Int = configuration.application.httpPort
    override val healthChecks: List<HealthCheck> = listOf(journalføringArena.arenaClient as HealthCheck)

    override fun filterPredicates(): List<Predicate<String, Packet>> =
        listOf(Predicate { _, packet -> !packet.hasField(PacketKeys.JOURNALPOST_ID) })

    override fun onPacket(packet: Packet) = journalføringArena.handlePacket(packet)

    override fun onFailure(packet: Packet, error: Throwable?): Packet {
        logger.error(error) { "Feilet ved håntering av pakke $packet" }
        throw error ?: RuntimeException("Feilet ved håndtering av pakke, ukjent grunn")
    }

    override fun getConfig(): Properties {
        val properties = streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = configuration.kafka.brokers,
            credential = configuration.kafka.credential()
        )
        properties[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = configuration.kafka.processingGuarantee
        return properties
    }
}

fun main(args: Array<String>) {
    val configuration = Configuration()

    val ytelseskontraktV3: YtelseskontraktV3 =
        SoapPort.ytelseskontraktV3(configuration.ytelseskontraktV3Config.endpoint)

    val behandleArbeidsytelseSak =
        SoapPort.behandleArbeidOgAktivitetOppgaveV1(configuration.behandleArbeidsytelseSakConfig.endpoint)

    val arenaClient: ArenaClient =
        SoapArenaClient(behandleArbeidsytelseSak, ytelseskontraktV3)

    val soapStsClient = stsClient(
        stsUrl = configuration.soapSTSClient.endpoint,
        credentials = configuration.soapSTSClient.username to configuration.soapSTSClient.password
    )
    if (configuration.soapSTSClient.allowInsecureSoapRequests) {
        soapStsClient.configureFor(behandleArbeidsytelseSak, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
        soapStsClient.configureFor(ytelseskontraktV3, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
    } else {
        soapStsClient.configureFor(behandleArbeidsytelseSak)
        soapStsClient.configureFor(ytelseskontraktV3)
    }

    val unleash: Unleash = DefaultUnleash(configuration.unleashConfig)

    val defaultStrategy =
        ArenaDefaultStrategy(
            listOf(
                ArenaCreateOppgaveStrategy(
                    arenaClient = arenaClient,
                    unleash = unleash
                ),
                ArenaKanIkkeOppretteOppgaveStrategy()
            )
        )

    val journalføringArena = JournalføringArena(defaultStrategy = defaultStrategy, arenaClient = arenaClient)

    val service = Application(configuration, journalføringArena)

    service.start()
}

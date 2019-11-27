package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import no.nav.dagpenger.journalføring.arena.adapter.soap.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.dagpenger.journalføring.arena.adapter.soap.SoapPort
import no.nav.dagpenger.journalføring.arena.adapter.soap.arena.SoapArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.soap.configureFor
import no.nav.dagpenger.journalføring.arena.adapter.soap.stsClient
import no.nav.dagpenger.streams.HealthCheck
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3
import org.apache.kafka.streams.kstream.Predicate
import java.util.Properties

private val logger = KotlinLogging.logger {}

internal object PacketKeys {
    const val ARENA_SAK_OPPRETTET: String = "arenaSakOpprettet"
    const val JOURNALPOST_ID: String = "journalpostId"
    const val BEHANDLENDE_ENHETER: String = "behandlendeEnheter"
    const val NATURLIG_IDENT: String = "naturligIdent"
    const val ARENA_SAK_ID: String = "arenaSakId"
}

class JournalføringArena(
    private val configuration: Configuration,
    private val defaultStrategy: ArenaDefaultStrategy,
    private val arenaClient: ArenaClient
) :
    River(configuration.kafka.dagpengerJournalpostTopic) {

    override val SERVICE_APP_ID = "dp-journalforing-arena"
    override val HTTP_PORT: Int = configuration.application.httpPort
    override val healthChecks: List<HealthCheck> = listOf(arenaClient as HealthCheck)

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(PacketKeys.ARENA_SAK_OPPRETTET) },
            Predicate { _, packet -> packet.hasField(PacketKeys.NATURLIG_IDENT) },
            Predicate { _, packet -> packet.hasField(PacketKeys.BEHANDLENDE_ENHETER) }
        )
    }

    override fun onPacket(packet: Packet): Packet {

        val naturligIdent: String = packet.getStringValue(PacketKeys.NATURLIG_IDENT)
        val enhetId =
            packet.getObjectValue(PacketKeys.BEHANDLENDE_ENHETER) { behandlendeenhetAdapter.fromJsonValue(it)!! }
                .first().enhetId

        val saker = arenaClient.hentArenaSaker(naturligIdent)

        val fakta = Fakta(naturligIdent = naturligIdent, enhetId = enhetId, arenaSaker = saker)

        val arenaSakId = defaultStrategy.handle(fakta)

        if (arenaSakId != null) {
            packet.putValue(PacketKeys.ARENA_SAK_OPPRETTET, true)
            packet.putValue(PacketKeys.ARENA_SAK_ID, arenaSakId.id)
            automatiskJournalførtJaTeller.inc()
        } else {
            packet.putValue(PacketKeys.ARENA_SAK_OPPRETTET, false)
        }
        registrerMetrikker(saker)
        saker.forEach {
            logger.info { "Tilhører sak: id: ${it.fagsystemSakId}, status: ${it.status}" }
        }
        logger.info {
            "Innsender av journalpost ${packet.getStringValue(PacketKeys.JOURNALPOST_ID)} har ${saker.size} dagpengesaker siste 104 uker"
        }

        return packet
    }

    override fun onFailure(packet: Packet, error: Throwable?): Packet {
        logger.error(error) { "Feilet ved håntering av pakke $packet" }
        throw error ?: RuntimeException("Feilet ved håndtering av pakke, ukjent grunn")
    }

    private fun registrerMetrikker(saker: List<ArenaSak>) {
        saker.filter { it.status == ArenaSakStatus.Aktiv }.also { aktiveDagpengeSakTeller.inc(it.size.toDouble()) }
        saker.filter { it.status == ArenaSakStatus.Lukket }.also { avsluttetDagpengeSakTeller.inc(it.size.toDouble()) }
        saker.filter { it.status == ArenaSakStatus.Inaktiv }.also { inaktivDagpengeSakTeller.inc(it.size.toDouble()) }
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

    val service = JournalføringArena(configuration, defaultStrategy, arenaClient)

    service.start()
}

package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.HentArenaSakerException
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
    const val NY_SØKNAD: String = "nySøknad"
    const val JOURNALPOST_ID: String = "journalpostId"
    const val AKTØR_ID: String = "aktørId"
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
            Predicate { _, packet -> !packet.hasField(PacketKeys.ARENA_SAK_OPPRETTET) }
        )
    }

    override fun onPacket(packet: Packet): Packet {

        val naturligIdent: String = packet.getStringValue(PacketKeys.NATURLIG_IDENT)
        val enhetId =
            packet.getObjectValue(PacketKeys.BEHANDLENDE_ENHETER) { behandlendeenhetAdapter.fromJsonValue(it)!! }
                .first().enhetId

        try {

            val saker = arenaClient.hentArenaSaker(naturligIdent)

            val fakta = Fakta(naturligIdent = naturligIdent, enhetId = enhetId, arenaSaker = saker)

            val arenaResultat = defaultStrategy.handle(fakta)

            packet.putValue(PacketKeys.ARENA_SAK_OPPRETTET, arenaResultat.opprettet)
            arenaResultat.arenaSakId?.let { packet.putValue(PacketKeys.ARENA_SAK_ID, it) }

            val aktiveSaker =
                saker.filter { it.status == "AKTIV" }.also { aktiveDagpengeSakTeller.inc(it.size.toDouble()) }
            saker.filter { it.status == "AVSLU" }.also { avsluttetDagpengeSakTeller.inc(it.size.toDouble()) }
            saker.filter { it.status == "INAKT" }.also { inaktivDagpengeSakTeller.inc(it.size.toDouble()) }

            if (aktiveSaker.isEmpty()) {
                automatiskJournalførtJaTeller.inc()
            } else {
                automatiskJournalførtNeiTeller.inc()
            }
            saker.forEach {
                logger.info { "Tilhører sak: id: ${it.fagsystemSakId}, status: ${it.status}" }
            }

            logger.info {
                "Innsender av journalpost ${packet.getStringValue(PacketKeys.JOURNALPOST_ID)} har ${saker.size} dagpengesaker siste 104 uker"
            }
        } catch (exception: HentArenaSakerException) {
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
                    unleash = unleash,
                    profile = configuration.application.profile
                ),
                ArenaKanIkkeOppretteOppgaveStrategy()
            )
        )

    val service = JournalføringArena(configuration, defaultStrategy, arenaClient)

    service.start()
}

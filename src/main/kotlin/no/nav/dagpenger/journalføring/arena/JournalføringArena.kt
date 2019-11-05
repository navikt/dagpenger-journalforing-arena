package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaOppgaveClient
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
    const val ARENA_SAK_RESULTAT: String = "arenaSakResultat"
}

class JournalføringArena(private val configuration: Configuration, val arenaOppgaveClient: ArenaOppgaveClient) :
    River(configuration.kafka.dagpengerJournalpostTopic) {

    override val SERVICE_APP_ID = "dp-journalforing-arena"
    override val HTTP_PORT: Int = configuration.application.httpPort

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(PacketKeys.ARENA_SAK_RESULTAT) }
        )
    }

    override fun onPacket(packet: Packet): Packet {

        val naturligIdent: String = packet.getStringValue(PacketKeys.NATURLIG_IDENT)
        val enhetId =
            packet.getObjectValue(PacketKeys.BEHANDLENDE_ENHETER) { behandlendeenhetAdapter.fromJsonValue(it)!! }
                .first().enhetId

        packet.putValue(
            PacketKeys.ARENA_SAK_RESULTAT,
            arenaOppgaveClient.bestillOppgave(naturligIdent = naturligIdent, behandlendeEnhetId = enhetId)
        )
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

    val service = if (configuration.application.profile != Profile.PROD) {
        // val behandleArbeidsytelseSak =
        //     SoapPort.BehandleArbeidOgAktivitetOppgaveV1(configuration.behandleArbeidsytelseSak.endpoint)
        //
        // val arenaOppgaveClient: ArenaOppgaveClient =
        //     SoapArenaOppgaveClient(behandleArbeidsytelseSak)
        //
        // val soapStsClient = stsClient(
        //     stsUrl = configuration.soapSTSClient.endpoint,
        //     credentials = configuration.soapSTSClient.username to configuration.soapSTSClient.password
        // ).also {
        //     it.configureFor(behandleArbeidsytelseSak)
        // }
        JournalføringArena(configuration, DummyArenaOppgaveClient())
    } else {
        JournalføringArena(configuration, DummyArenaOppgaveClient())
    }

    service.start()
}

class DummyArenaOppgaveClient : ArenaOppgaveClient {
    override fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String {
        return "DUMMY_SAK!"
    }
}

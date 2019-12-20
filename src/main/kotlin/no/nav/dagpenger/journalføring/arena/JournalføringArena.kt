package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import org.apache.kafka.streams.kstream.Predicate

private val logger = KotlinLogging.logger {}

internal object PacketKeys {
    const val DOKUMENTER: String = "dokumenter"
    const val DATO_REGISTRERT: String = "datoRegistrert"
    const val ARENA_SAK_OPPRETTET: String = "arenaSakOpprettet"
    const val JOURNALPOST_ID: String = "journalpostId"
    const val BEHANDLENDE_ENHET: String = "behandlendeEnhet"
    const val NATURLIG_IDENT: String = "naturligIdent"
    const val ARENA_SAK_ID: String = "arenaSakId"
    const val TOGGLE_BEHANDLE_NY_SØKNAD: String = "toggleBehandleNySøknad"
}

val filterPredicates = listOf<Predicate<String, Packet>>(
    Predicate { _, packet -> packet.hasEnabledToggle() },
    Predicate { _, packet -> !packet.hasField(PacketKeys.ARENA_SAK_OPPRETTET) },
    Predicate { _, packet -> packet.hasField(PacketKeys.NATURLIG_IDENT) },
    Predicate { _, packet -> packet.hasField(PacketKeys.BEHANDLENDE_ENHET) }
)

private fun Packet.hasEnabledToggle() =
    this.hasField(PacketKeys.TOGGLE_BEHANDLE_NY_SØKNAD) && this.getBoolean(PacketKeys.TOGGLE_BEHANDLE_NY_SØKNAD)

internal class JournalføringArena(private val defaultStrategy: ArenaStrategy, val arenaClient: ArenaClient) {

    private fun naturligIdentFrom(packet: Packet) = packet.getStringValue(PacketKeys.NATURLIG_IDENT)
    private fun behandlendeEnhetFrom(packet: Packet) = packet.getStringValue(PacketKeys.BEHANDLENDE_ENHET)
    private fun journalpostIdFrom(packet: Packet) = packet.getStringValue(PacketKeys.JOURNALPOST_ID)
    private fun registrertDatoFrom(packet: Packet) = packet.getStringValue(PacketKeys.DATO_REGISTRERT)
    private fun dokumentTitlerFrom(packet: Packet) =
        packet.getObjectValue(PacketKeys.DOKUMENTER) { dokumentAdapter.fromJsonValue(it)!! }.map { it.tittel }

    private fun Packet.setArenaSakOpprettet(sakOpprettet: Boolean) =
        this.putValue(PacketKeys.ARENA_SAK_OPPRETTET, sakOpprettet)

    private fun Packet.setArenaSakId(arenaSakId: String) = this.putValue(PacketKeys.ARENA_SAK_ID, arenaSakId)

    fun handlePacket(packet: Packet): Packet {
        val naturligIdent = naturligIdentFrom(packet)

        val saker = arenaClient.hentArenaSaker(naturligIdent).also {
            registrerMetrikker(it)
            logger.info {
                "Innsender av journalpost ${journalpostIdFrom(packet)} har ${it.filter { it.status == ArenaSakStatus.Aktiv }.size} aktive saker av ${it.size} dagpengesaker totalt"
            }
        }

        val fakta = Fakta(
            naturligIdent = naturligIdent,
            enhetId = behandlendeEnhetFrom(packet),
            arenaSaker = saker,
            journalpostId = journalpostIdFrom(packet),
            dokumentTitler = dokumentTitlerFrom(packet),
            registrertDato = registrertDatoFrom(packet)
        )

        defaultStrategy.handle(fakta).apply {
            val sakBleOpprettet = this != null
            packet.setArenaSakOpprettet(sakBleOpprettet)

            if (sakBleOpprettet) packet.setArenaSakId(this!!.id)
        }

        return packet
    }
}

private fun registrerMetrikker(saker: List<ArenaSak>) {
    saker.filter { it.status == ArenaSakStatus.Aktiv }.also { aktiveDagpengeSakTeller.inc(it.size.toDouble()) }
    saker.filter { it.status == ArenaSakStatus.Lukket }.also { avsluttetDagpengeSakTeller.inc(it.size.toDouble()) }
    saker.filter { it.status == ArenaSakStatus.Inaktiv }.also { inaktivDagpengeSakTeller.inc(it.size.toDouble()) }
}

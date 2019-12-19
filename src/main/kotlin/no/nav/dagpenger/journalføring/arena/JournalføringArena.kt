package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus

private val logger = KotlinLogging.logger {}

internal class JournalføringArena(private val defaultStrategy: ArenaStrategy, val arenaClient: ArenaClient) {
    fun handlePacket(packet: Packet): Packet {
        val naturligIdent: String = packet.getStringValue(PacketKeys.NATURLIG_IDENT)
        val journalpostId = packet.getStringValue(PacketKeys.JOURNALPOST_ID)
        val registrertDato: String = packet.getStringValue(PacketKeys.DATO_REGISTRERT)
        val dokumentTitler =
            packet.getObjectValue(PacketKeys.DOKUMENTER) { dokumentAdapter.fromJsonValue(it)!! }.map { it.tittel }
        val enhetId =
            packet.getStringValue(PacketKeys.BEHANDLENDE_ENHET)

        val saker = arenaClient.hentArenaSaker(naturligIdent)

        val fakta =
            Fakta(
                naturligIdent = naturligIdent,
                enhetId = enhetId,
                arenaSaker = saker,
                journalpostId = journalpostId,
                dokumentTitler = dokumentTitler,
                registrertDato = registrertDato
            )

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
            "Innsender av journalpost ${packet.getStringValue(PacketKeys.JOURNALPOST_ID)} har ${saker.filter { it.status == ArenaSakStatus.Aktiv }.size} aktive saker av ${saker.size} dagpengesaker totalt"
        }

        return packet
    }
}

private fun registrerMetrikker(saker: List<ArenaSak>) {
    saker.filter { it.status == ArenaSakStatus.Aktiv }.also { aktiveDagpengeSakTeller.inc(it.size.toDouble()) }
    saker.filter { it.status == ArenaSakStatus.Lukket }.also { avsluttetDagpengeSakTeller.inc(it.size.toDouble()) }
    saker.filter { it.status == ArenaSakStatus.Inaktiv }.also { inaktivDagpengeSakTeller.inc(it.size.toDouble()) }
}

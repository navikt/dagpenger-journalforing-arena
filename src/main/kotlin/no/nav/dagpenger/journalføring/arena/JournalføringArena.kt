package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.River
import org.apache.kafka.streams.kstream.Predicate

private val logger = KotlinLogging.logger {}

class JournalføringArena(val configuration: Configuration) : River(configuration.kafka.dagpengerJournalpostTopic) {

    override val SERVICE_APP_ID = "journalføring-arena"
    override val HTTP_PORT: Int = configuration.application.httpPort

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField("arena-journalføring") }
        )
    }

    override fun onPacket(packet: Packet): Packet {

        logger.info { "GOT VALUE $packet" }

        packet.putValue("arena-journalføring", "yes")
        return packet
    }
}

fun main(args: Array<String>) {
    val service = JournalføringArena(Configuration())
    service.start()
}

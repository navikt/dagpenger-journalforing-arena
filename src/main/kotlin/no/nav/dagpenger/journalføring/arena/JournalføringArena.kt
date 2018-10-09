package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.toTopic
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder

private val LOGGER = KotlinLogging.logger {}

class JournalføringArena() : Service() {
    override val SERVICE_APP_ID = "journalføring-arena"
    override val HTTP_PORT: Int = 8080

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val service = JournalføringArena()
            service.start()
        }
    }

    override fun setupStreams(): KafkaStreams {
        println(SERVICE_APP_ID)
        val builder = StreamsBuilder()

        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST)

        inngåendeJournalposter
                .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
                .filter { _, behov -> behov.getJournalpost().getJournalpostType() != null }
                .filter { _, behov -> behov.getJournalpost().getBehandleneEnhet() != null }
                .filter { _, behov -> behov.getJournalpost().getFagsakId() == null }
                .mapValues(this::addFagsakId)
                .peek { key, value -> LOGGER.info("Producing ${value.javaClass} with key $key") }
                .toTopic(INNGÅENDE_JOURNALPOST)

        return KafkaStreams(builder.build(), this.getConfig())
    }

    private fun addFagsakId(behov: Behov): Behov {
        val journalpost = behov.getJournalpost()
        return behov
    }
}

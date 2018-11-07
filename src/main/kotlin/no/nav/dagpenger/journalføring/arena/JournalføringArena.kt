package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.JournalpostType
import no.nav.dagpenger.events.avro.JournalpostType.ETTERSENDING
import no.nav.dagpenger.events.avro.JournalpostType.GJENOPPTAK
import no.nav.dagpenger.events.avro.JournalpostType.MANUELL
import no.nav.dagpenger.events.avro.JournalpostType.NY
import no.nav.dagpenger.events.avro.JournalpostType.UKJENT
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class JournalføringArena(val env: Environment) : Service() {
    override val SERVICE_APP_ID = "journalføring-arena"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = Environment()
            val service = JournalføringArena(env)
            service.start()
        }
    }

    override fun setupStreams(): KafkaStreams {
        println(SERVICE_APP_ID)
        val builder = StreamsBuilder()

        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST)

        inngåendeJournalposter
            .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
            .filter { _, behov -> behov.getJournalpost().getBehandleneEnhet() != null }
            .filter { _, behov -> behov.getJournalpost().getFagsakId() == null }
            .filter { _, behov -> behov.getJournalpost().getJournalpostType() != null }
            .filter { _, behov -> filterJournalpostTypes(behov.getJournalpost().getJournalpostType()) }
            .mapValues(this::addFagsakId)
            .peek { key, value -> LOGGER.info("Producing ${value.javaClass} with key $key") }
            .toTopic(INNGÅENDE_JOURNALPOST)

        return KafkaStreams(builder.build(), this.getConfig())
    }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password)
        )
    }

    private fun filterJournalpostTypes(journalpostType: JournalpostType): Boolean {
        return when (journalpostType) {
            NY, GJENOPPTAK, ETTERSENDING -> true
            UKJENT, MANUELL -> false
        }
    }

    private fun addFagsakId(behov: Behov): Behov {
        val journalpost = behov.getJournalpost()

        val sakId = when (journalpost.getJournalpostType()) {
            NY -> createSak(journalpost.getBehandleneEnhet(), journalpost.getSøker().getIdentifikator())
            ETTERSENDING, GJENOPPTAK -> findSakAndCreateOppgave(
                journalpost.getBehandleneEnhet(),
                journalpost.getSøker().getIdentifikator()
            )
            else -> throw UnexpectedJournaltypeException("Unexpected journalposttype ${journalpost.getJournalpostType()}")
        }

        journalpost.setFagsakId(sakId)
        return behov
    }

    private fun createSak(behandlendeEnhet: String, fødselsnummer: String): String {
        return ""
    }

    private fun findSakAndCreateOppgave(behandlendeEnhet: String, fødselsnummer: String): String {
        return ""
    }
}

class UnexpectedJournaltypeException(override val message: String) : RuntimeException(message)

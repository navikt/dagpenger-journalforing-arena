package no.nav.dagpenger.journalføring.arena

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.hasBehandlendeEnhet
import no.nav.dagpenger.events.hasFagsakId
import no.nav.dagpenger.events.isEttersending
import no.nav.dagpenger.events.isGjenopptakSoknad
import no.nav.dagpenger.events.isNySoknad
import no.nav.dagpenger.events.isSoknad
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import no.nav.dagpenger.streams.configureAvroSerde
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class JournalføringArena(val env: Environment, val oppslagClient: OppslagClient) : Service() {
    override val SERVICE_APP_ID = "journalføring-arena"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = Environment()
            val oppslagHttpClient = OppslagHttpClient(env.dagpengerOppslagUrl)
            val service = JournalføringArena(env, oppslagHttpClient)
            service.start()
        }
    }

    override fun setupStreams(): KafkaStreams {
        println(SERVICE_APP_ID)

        val innkommendeJournalpost = INNGÅENDE_JOURNALPOST.copy(
            valueSerde = configureAvroSerde<Behov>(
                mapOf(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl)
            )
        )

        val builder = StreamsBuilder()
        val inngåendeJournalposter = builder.consumeTopic(innkommendeJournalpost)

        inngåendeJournalposter
            .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
            .filter { _, behov -> shouldBeProcessed(behov) }
            .mapValues(this::addFagsakId)
            .peek { key, value -> LOGGER.info("Producing ${value.javaClass} with key $key") }
            .toTopic(innkommendeJournalpost)

        return KafkaStreams(builder.build(), this.getConfig())
    }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password)
        )
    }

    private fun shouldBeProcessed(behov: Behov): Boolean {
        return !behov.getTrengerManuellBehandling()
            && behov.hasBehandlendeEnhet()
            && !behov.hasFagsakId()
            && (behov.isSoknad() || behov.isEttersending())
    }

    private fun addFagsakId(behov: Behov): Behov {

        if (behov.isNySoknad()) {
            createNewSak(behov)
        } else if (behov.isGjenopptakSoknad() || behov.isEttersending()) {
            findSakAndCreateOppgave(behov)
        }
        return behov
    }

    private fun createNewSak(behov: Behov) {
        val createNewOppgaveAndSak =
            CreateArenaOppgaveRequest(
                behov.getBehandleneEnhet(),
                behov.getMottaker().getIdentifikator(),
                null,
                "STARTVEDTAK",
                true
            )

        val sakId = oppslagClient.createOppgave(createNewOppgaveAndSak)

        behov.setFagsakId(sakId)
    }

    private fun findSakAndCreateOppgave(behov: Behov) {

        val sakId = findNewestActiveDagpengerSak(behov.getMottaker().getIdentifikator())

        if (sakId == null) {
            // TODO: find out how to send behov to be manually processed
            throw RuntimeException("Could not find any existing arena-sak")
        } else {
            val createNewOppgaveOnExistingSak =
                CreateArenaOppgaveRequest(
                    behov.getBehandleneEnhet(),
                    behov.getMottaker().getIdentifikator(),
                    sakId,
                    "BEHENVPERSON",
                    false
                )

            oppslagClient.createOppgave(createNewOppgaveOnExistingSak)

            behov.setFagsakId(sakId)
        }
    }

    private fun findNewestActiveDagpengerSak(fødselsnummer: String): String? {
        val getActiveDagpengerSaker = GetArenaSakerRequest(fødselsnummer, "PERSON", "DAG", false)

        val saker: List<ArenaSak> = oppslagClient.getSaker(getActiveDagpengerSaker)

        return saker.filter { it.sakstatus == "AKTIV" }.maxBy { it.sakOpprettet }?.sakId
    }
}


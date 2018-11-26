package no.nav.dagpenger.journalføring.arena

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
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Predicate
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

        val builder = StreamsBuilder()
        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST, env.schemaRegistryUrl)

        val (needsNewArenaSak, hasExistingArenaSak) = inngåendeJournalposter
            .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
            .filter { _, behov -> shouldBeProcessed(behov) }
            .kbranch(
                { _, behov -> behov.isNySoknad() },
                { _, behov -> behov.isGjenopptakSoknad() || behov.isEttersending() })

        needsNewArenaSak.mapValues(this::createNewSak)

        hasExistingArenaSak.mapValues(this::findSakAndCreateOppgave)

        needsNewArenaSak.merge(hasExistingArenaSak)
            .peek { key, value -> LOGGER.info("Producing ${value.javaClass} with key $key") }
            .toTopic(INNGÅENDE_JOURNALPOST, env.schemaRegistryUrl)

        return KafkaStreams(builder.build(), this.getConfig())
    }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password)
        )
    }

    fun createNewSak(behov: Behov) {
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

    fun findSakAndCreateOppgave(behov: Behov) {
        val sakId = findNewestActiveDagpengerSak(behov.getMottaker().getIdentifikator())

        if (sakId == null) {
            LOGGER.error { "Could not find sak in Arena for JournalpostId=${behov.getJournalpost().getJournalpostId()}, setting trengerManuellBehandling = true." }
            behov.setTrengerManuellBehandling(true)
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

    fun findNewestActiveDagpengerSak(fødselsnummer: String): String? {
        val getActiveDagpengerSaker = GetArenaSakerRequest(fødselsnummer, "PERSON", "DAG", false)

        val saker: List<ArenaSak> = oppslagClient.getSaker(getActiveDagpengerSaker)

        return saker.filter { it.sakstatus == "AKTIV" }.maxBy { it.sakOpprettet }?.sakId
    }

    // https://stackoverflow.com/a/48048516/10075690
    fun <K, V> KStream<K, V>.kbranch(vararg predicates: (K, V) -> Boolean): Array<KStream<K, V>> {
        val arguments = predicates.map { Predicate { key: K, value: V -> it(key, value) } }
        return this.branch(*arguments.toTypedArray())
    }
}

fun shouldBeProcessed(behov: Behov): Boolean {
    return !behov.getTrengerManuellBehandling()
        && behov.hasBehandlendeEnhet()
        && !behov.hasFagsakId()
        && (behov.isSoknad() || behov.isEttersending())
}


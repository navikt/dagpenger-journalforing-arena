package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.finn.unleash.Unleash
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.streams.PacketDeserializer
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topic
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test
import java.util.Properties

class JournalFøringArenaTopologyTest {

    val dagpengerJournalpostTopic: Topic<String, Packet> = Topic(
        "privat-dagpenger-journalpost-mottatt-v1",
        keySerde = Serdes.String(),
        valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
    )

    val factory = ConsumerRecordFactory<String, Packet>(
        dagpengerJournalpostTopic.name,
        dagpengerJournalpostTopic.keySerde.serializer(),
        dagpengerJournalpostTopic.valueSerde.serializer()
    )

    val properties = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    }

    @Test
    fun `Skal prosessere melding hvis arena resultat mangler `() {

        val arenaOppgaveClient: ArenaClient = mockk()
        every {
            arenaOppgaveClient.bestillOppgave("12345678", "1234")
        } returns "1234"

        every {
            arenaOppgaveClient.hentArenaSaker("12345678")
        } returns emptyList<ArenaSak>()

        val unleashMock: Unleash = mockk()
        every {
            unleashMock.isEnabled("dp-arena.bestillOppgaveLOCAL", false)
        } returns true

        val testService = JournalføringArena(Configuration(), arenaOppgaveClient, unleashMock)

        val packet = Packet().apply {
            putValue(
                "behandlendeEnheter", behandlendeenhetAdapter.toJsonValue(
                    listOf(
                        Behandlendeenhet(
                            enhetId = "1234",
                            enhetNavn = "NAV"
                        )
                    )
                )!!
            )
            putValue("naturligIdent", "12345678")
        }

        TopologyTestDriver(testService.buildTopology(), properties).use { topologyTestDriver ->
            val inputRecord = factory.create(packet)
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                dagpengerJournalpostTopic.name,
                dagpengerJournalpostTopic.keySerde.deserializer(),
                dagpengerJournalpostTopic.valueSerde.deserializer()
            )

            ut shouldNotBe null
            ut.value().hasProblem() shouldBe false
            ut.value().hasField("arenaSakId") shouldNotBe false
            ut.value().getStringValue("arenaSakId") shouldBe "1234"
        }

        /*  val registry = CollectorRegistry.defaultRegistry


          registry.metricFamilySamples().asSequence().find { it.name == "automatisk_journalfort_arena" }?.let { metric ->
              print(metric)
              metric.samples[0].value shouldNotBe null
              metric.samples[0].value shouldBeGreaterThan 0.0
              metric.samples[0].labelValues[0] shouldBe "true"
          }
  */

        verify { arenaOppgaveClient.bestillOppgave("12345678", "1234") }
    }

    @Test
    fun `Skal ikke legge på arenaSakId hvis bruker har aktiv sak i Arena`() {

        val arenaOppgaveClient: ArenaClient = mockk()
        every {
            arenaOppgaveClient.hentArenaSaker("12345678")
        } returns listOf(ArenaSak(999, "AKTIV"))

        val unleashMock: Unleash = mockk()
        every {
            unleashMock.isEnabled("dp-arena.bestillOppgaveLOCAL", false)
        } returns true

        val testService = JournalføringArena(Configuration(), arenaOppgaveClient, unleashMock)

        val packet = Packet().apply {
            putValue(
                "behandlendeEnheter", behandlendeenhetAdapter.toJsonValue(
                    listOf(
                        Behandlendeenhet(
                            enhetId = "1234",
                            enhetNavn = "NAV"
                        )
                    )
                )!!
            )
            putValue("naturligIdent", "12345678")
        }

        TopologyTestDriver(testService.buildTopology(), properties).use { topologyTestDriver ->
            val inputRecord = factory.create(packet)
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                dagpengerJournalpostTopic.name,
                dagpengerJournalpostTopic.keySerde.deserializer(),
                dagpengerJournalpostTopic.valueSerde.deserializer()
            )

            ut shouldNotBe null
            ut.value().hasProblem() shouldBe false
            ut.value().hasField(PacketKeys.ARENA_SAK_ID) shouldBe false
            ut.value().getBoolean(PacketKeys.ARENA_SAK_OPPRETTET) shouldBe false
        }
    }

    @Test
    fun `Skal ikke prosessere meldinger hvor arenasak er forsøkt opprettet`() {

        val testService = JournalføringArena(Configuration(), mockk(), mockk())

        val packet = Packet().apply {
            putValue(
                "behandlendeEnheter", behandlendeenhetAdapter.toJsonValue(
                    listOf(
                        Behandlendeenhet(
                            enhetId = "1234",
                            enhetNavn = "NAV"
                        )
                    )
                )!!
            )
            putValue("naturligIdent", "12345678")
            putValue("arenaSakOpprettet", true)
        }

        TopologyTestDriver(testService.buildTopology(), properties).use { topologyTestDriver ->
            val inputRecord = factory.create(packet)
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                dagpengerJournalpostTopic.name,
                dagpengerJournalpostTopic.keySerde.deserializer(),
                dagpengerJournalpostTopic.valueSerde.deserializer()
            )

            ut shouldBe null
        }
    }
}
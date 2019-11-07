package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
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

        val testService = JournalføringArena(Configuration(), arenaOppgaveClient)

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

        // verify { arenaOppgaveClient.bestillOppgave("12345678", "1234") }
    }
}
package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakId
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

    val packetWithTrueToggle = Packet().apply { putValue(PacketKeys.TOGGLE_BEHANDLE_NY_SØKNAD, true) }

    @Test
    fun `Skal prosessere melding hvis arena resultat mangler `() {

        val arenaOppgaveClient: ArenaClient = mockk()

        every {
            arenaOppgaveClient.hentArenaSaker("12345678")
        } returns emptyList<ArenaSak>()

        val mockedStrategy: ArenaDefaultStrategy = mockk()
        every {
            mockedStrategy.handle(any())
        } returns ArenaSakId("1234")

        val testService = Application(Configuration(), mockedStrategy, arenaOppgaveClient)

        val packet = packetWithTrueToggle.apply {
            putValue("behandlendeEnhet", "1234")
            putValue("naturligIdent", "12345678")
            putValue("journalpostId", "666")
            putValue("dokumenter", listOf(Dokument("Søknad 1")))
            putValue("datoRegistrert", "2019")
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
    }

    @Test
    fun `Skal ikke prosessere meldinger hvor arenasak er forsøkt opprettet`() {

        val testService = Application(Configuration(), mockk(), mockk())

        val packet = packetWithTrueToggle.apply {
            putValue("behandlendeEnhet", "1234")
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

    @Test
    fun `skal kaste feil hvis det skjer en ukjent feil`() {

        val feilendeArenaKlient = mockk<ArenaClient>()

        every { feilendeArenaKlient.hentArenaSaker(any()) } throws RuntimeException()

        val testService = Application(Configuration(), mockk(), feilendeArenaKlient)

        val packet = packetWithTrueToggle.apply {
            putValue("behandlendeEnhet", "1234")
            putValue("naturligIdent", "12345678")
        }

        shouldThrow<java.lang.RuntimeException> {
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

    @Test
    fun `skal ikke behandle pakker uten naturlig ident`() {

        val service = Application(Configuration(), mockk(), mockk())

        val packet = packetWithTrueToggle.apply {
            putValue("behandlendeEnhet", "tomListe")
        }

        service.filterPredicates().all { it.test("", packet) } shouldBe false
    }

    @Test
    fun `skal ikke behandle pakker uten behandlendeEnhet`() {

        val service = Application(Configuration(), mockk(), mockk())

        val packet = packetWithTrueToggle.apply {
            putValue("naturligIdent", "1234")
        }

        service.filterPredicates().all { it.test("", packet) } shouldBe false
    }

    @Test
    fun `skal behandle pakken hvis behandlendeEnhet og naturligIdent finnes, men ikke arenaResultat`() {
        val service = Application(Configuration(), mockk(), mockk())

        val packet = packetWithTrueToggle.apply {
            putValue("naturligIdent", "1234")
            putValue("behandlendeEnhet", "")
        }

        service.filterPredicates().all { it.test("", packet) } shouldBe true
    }

    @Test
    fun `Skal ikke behandle pakken dersom feature toggle flag ikke finnes`() {
        val service = Application(Configuration(), mockk(), mockk())
        val packet = Packet().apply {
            putValue("naturligIdent", "1234")
            putValue("behandlendeEnhet", "")
        }

        service.filterPredicates().all { it.test("", packet) } shouldBe false
    }

    @Test
    fun `Skal ikke behandle pakken dersom feature toggle flag er false `() {
        val service = Application(Configuration(), mockk(), mockk())
        val packet = Packet().apply {
            putValue("naturligIdent", "1234")
            putValue("behandlendeEnhet", "")
            putValue(PacketKeys.TOGGLE_BEHANDLE_NY_SØKNAD, false)
        }
        service.filterPredicates().all { it.test("", packet) } shouldBe false
    }
}
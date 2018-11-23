package no.nav.dagpenger.journalføring.arena

import no.nav.dagpenger.events.avro.Annet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Ettersending
import no.nav.dagpenger.events.avro.HenvendelsesType
import no.nav.dagpenger.events.avro.Søknad
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JournalforingArenaTest {

    lateinit var journalføringArena: JournalføringArena

    val nySøknad: HenvendelsesType =
        HenvendelsesType
            .newBuilder()
            .setSøknad(
                Søknad
                    .newBuilder()
                    .setVedtakstype("NY")
                    .build()
            )
            .build()

    val gjenopptakSøknad: HenvendelsesType =
        HenvendelsesType
            .newBuilder()
            .setSøknad(
                Søknad
                    .newBuilder()
                    .setVedtakstype("GJENOPPTAK")
                    .build()
            )
            .build()

    val ettersending: HenvendelsesType = HenvendelsesType
        .newBuilder()
        .setEttersending(Ettersending())
        .build()

    val annet: HenvendelsesType = HenvendelsesType
        .newBuilder()
        .setAnnet(Annet())
        .build()

    @Before
    fun setUp() {
        val env = Environment("", "", "")
        journalføringArena = JournalføringArena(env, DummyOppslagClient())
    }

    fun createBehov(
        henvendelsesType: HenvendelsesType,
        hasBehandlendeEnhet: Boolean = false,
        hasFagsakId: Boolean = false,
        trengerManuellBehandling: Boolean = false
    ): Behov {
        val behov = Behov.newBuilder()
            .setBehovId("10")
            .setHenvendelsesType(henvendelsesType)
            .setTrengerManuellBehandling(trengerManuellBehandling)

        if (hasBehandlendeEnhet) behov.behandleneEnhet = "0000"
        if (hasFagsakId) behov.fagsakId = "1215"

        return behov.build()
    }

    @Test
    fun `should process ny søknad with behandlende enhet`() {
        assertTrue { journalføringArena.shouldBeProcessed(createBehov(nySøknad, true)) }
    }

    @Test
    fun `should process gjenopptak søknad with behandlendeenhet`() {
        assertTrue { journalføringArena.shouldBeProcessed(createBehov(gjenopptakSøknad, true)) }
    }

    @Test
    fun `should process ettersending with behandlende enhet`() {
        assertTrue { journalføringArena.shouldBeProcessed(createBehov(ettersending, true)) }
    }

    @Test
    fun `should not process annet with behandlende enhet`() {
        assertFalse { journalføringArena.shouldBeProcessed(createBehov(annet, true)) }
    }

    @Test
    fun `should not process behovs with fagsakId`() {
        listOf(nySøknad, gjenopptakSøknad, ettersending, annet).forEach {
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, true, true, true)) }
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, true, true, false)) }
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, false, true, true)) }
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, false, true, false)) }
        }
    }

    @Test
    fun `should not process behovs with trengerManuellBehandling`() {
        listOf(nySøknad, gjenopptakSøknad, ettersending, annet).forEach {
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, true, true, true)) }
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, true, false, true)) }
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, false, true, true)) }
            assertFalse { journalføringArena.shouldBeProcessed(createBehov(it, false, false, true)) }
        }
    }

    class DummyOppslagClient : OppslagClient {
        override fun createOppgave(request: CreateArenaOppgaveRequest): String {
            return "ArenaSakId"
        }

        override fun getSaker(request: GetArenaSakerRequest): List<ArenaSak> {
            return listOf()
        }
    }
}
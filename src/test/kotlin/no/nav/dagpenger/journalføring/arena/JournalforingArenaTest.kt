package no.nav.dagpenger.journalføring.arena

import no.nav.dagpenger.events.avro.Annet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Ettersending
import no.nav.dagpenger.events.avro.HenvendelsesType
import no.nav.dagpenger.events.avro.Mottaker
import no.nav.dagpenger.events.avro.Søknad
import no.nav.dagpenger.events.avro.Vedtakstype
import org.junit.Test
import org.mockito.Mockito
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JournalforingArenaTest {

    val nySøknad: HenvendelsesType =
        HenvendelsesType
            .newBuilder()
            .setSøknad(
                Søknad
                    .newBuilder()
                    .setVedtakstype(Vedtakstype.NY_RETTIGHET)
                    .build()
            )
            .build()

    val gjenopptakSøknad: HenvendelsesType =
        HenvendelsesType
            .newBuilder()
            .setSøknad(
                Søknad
                    .newBuilder()
                    .setVedtakstype(Vedtakstype.GJENOPPTAK)
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
            .setMottaker(Mottaker("id"))

        if (hasBehandlendeEnhet) behov.behandleneEnhet = "0000"
        if (hasFagsakId) behov.fagsakId = "1215"

        return behov.build()
    }

    @Test
    fun `should process ny søknad with behandlende enhet`() {
        assertTrue { shouldBeProcessed(createBehov(nySøknad, true)) }
    }

    @Test
    fun `should process gjenopptak søknad with behandlendeenhet`() {
        assertTrue { shouldBeProcessed(createBehov(gjenopptakSøknad, true)) }
    }

    @Test
    fun `should process ettersending with behandlende enhet`() {
        assertTrue { shouldBeProcessed(createBehov(ettersending, true)) }
    }

    @Test
    fun `should not process annet with behandlende enhet`() {
        assertFalse { shouldBeProcessed(createBehov(annet, true)) }
    }

    @Test
    fun `should not process behovs with fagsakId`() {
        listOf(nySøknad, gjenopptakSøknad, ettersending, annet).forEach {
            assertFalse { shouldBeProcessed(createBehov(it, true, true, true)) }
            assertFalse { shouldBeProcessed(createBehov(it, true, true, false)) }
            assertFalse { shouldBeProcessed(createBehov(it, false, true, true)) }
            assertFalse { shouldBeProcessed(createBehov(it, false, true, false)) }
        }
    }

    @Test
    fun `should not process behovs with trengerManuellBehandling`() {
        listOf(nySøknad, gjenopptakSøknad, ettersending, annet).forEach {
            assertFalse { shouldBeProcessed(createBehov(it, true, true, true)) }
            assertFalse { shouldBeProcessed(createBehov(it, true, false, true)) }
            assertFalse { shouldBeProcessed(createBehov(it, false, true, true)) }
            assertFalse { shouldBeProcessed(createBehov(it, false, false, true)) }
        }
    }

    /**
     * Returns Mockito.any() as nullable type to avoid java.lang.IllegalStateException when
     * null is returned.
     */
    fun <T> any(): T = Mockito.any<T>()

    @Test
    fun `findNewestActiveDagpengerSak should return most recent sak`() {
        val saker = listOf(
            ArenaSak("first", "AKTIV", Date(2018, 10, 10)),
            ArenaSak("last", "AKTIV", Date(2018, 11, 10)),
            ArenaSak("middle", "AKTIV", Date(2018, 10, 20)),
            ArenaSak("newestButInactive", "INAKTIV", Date(2018, 11, 20))
        )

        val oppslagMock = Mockito.mock(OppslagClient::class.java)
        Mockito.`when`(oppslagMock.getSaker(any())).thenReturn(saker)
        val journalføringArena = JournalføringArena(Environment("", "", ""), oppslagMock)

        assertEquals("last", journalføringArena.findNewestActiveDagpengerSak("123123"))
    }

    @Test
    fun `findNewestActiveDagpengerSak should return null if no saker found`() {
        val oppslagMock = Mockito.mock(OppslagClient::class.java)
        Mockito.`when`(oppslagMock.getSaker(any())).thenReturn(listOf())
        val journalføringArena = JournalføringArena(Environment("", "", ""), oppslagMock)

        assertNull(journalføringArena.findNewestActiveDagpengerSak("123123"))
    }

    @Test
    fun `findSakAndCreateOppgave sets manuellBhenalding flag if no saker are found`() {
        val oppslagMock = Mockito.mock(OppslagClient::class.java)
        Mockito.`when`(oppslagMock.getSaker(any())).thenReturn(listOf())
        val journalføringArena = JournalføringArena(Environment("", "", ""), oppslagMock)

        val behov = createBehov(gjenopptakSøknad, trengerManuellBehandling = false)

        journalføringArena.findSakAndCreateOppgave(behov)

        assertEquals(behov.getTrengerManuellBehandling(), true)
    }
}
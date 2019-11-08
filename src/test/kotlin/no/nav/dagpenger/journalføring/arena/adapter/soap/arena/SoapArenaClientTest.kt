package no.nav.dagpenger.journalføring.arena.adapter.soap.arena

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.arena.services.lib.sakvedtak.SaksInfo
import no.nav.arena.services.lib.sakvedtak.SaksInfoListe
import no.nav.arena.services.sakvedtakservice.SakVedtakPortType
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveResponse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.xml.ws.Holder

internal class SoapArenaClientTest {

    @Test
    fun `suksessfull bestillOppgave gir arenaSakId `() {
        val stubbedClient = mockk<BehandleArbeidOgAktivitetOppgaveV1>()
        every { stubbedClient.bestillOppgave(any()) } returns WSBestillOppgaveResponse().withArenaSakId("123")

        val client = SoapArenaClient(stubbedClient, mockk())

        val actual = client.bestillOppgave("123456789", "abcbscb")

        actual shouldBe "123"
    }

    @Test
    @Disabled
    fun `Skal kunne hente arena saker basert på bruker id`() {

        val sakVedtakPortType: SakVedtakPortType = mockk()

        val holderSlot = slot<Holder<SaksInfoListe>>()

        every {
            sakVedtakPortType.hentSaksInfoListeV2(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                capture(holderSlot)
            )
        } answers {
            val saksInfoListeHolder = Holder<SaksInfoListe>()
            saksInfoListeHolder.value = SaksInfoListe().apply {
                this.saksInfo.add(SaksInfo().apply {
                    saksId = "123"
                })
                holderSlot.captured = saksInfoListeHolder
            }

            // val client = SoapArenaClient(mockk(), sakVedtakPortType)
            //
            // val saker = client.hentArenaSaker(
            //     "1234"
            // )
            //
            // saker.size shouldNotBe 0
            // saker.first().saksId shouldBe "123"
        }
    }
}
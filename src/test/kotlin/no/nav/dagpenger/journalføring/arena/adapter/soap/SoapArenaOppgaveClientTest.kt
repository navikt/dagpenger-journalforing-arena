package no.nav.dagpenger.journalføring.arena.adapter.soap

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.journalføring.arena.adapter.soap.arena.SoapArenaOppgaveClient
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveResponse
import org.junit.jupiter.api.Test

class SoapArenaOppgaveClientTest {

    @Test
    fun `suksessfull bestillOppgave gir arenaSakId `() {
        val stubbedClient = mockk<BehandleArbeidOgAktivitetOppgaveV1>()
        every { stubbedClient.bestillOppgave(any()) } returns WSBestillOppgaveResponse().withArenaSakId("123")

        val client = SoapArenaOppgaveClient(stubbedClient)

        val actual = client.bestillOppgave("123456789", "abcbscb")

        actual shouldBe "123"
    }
}
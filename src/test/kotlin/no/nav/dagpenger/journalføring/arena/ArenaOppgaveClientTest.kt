package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveResponse
import org.junit.jupiter.api.Test

class ArenaOppgaveClientTest {


    @Test
    fun `suksessfull bestillOppgave gir arenaSakId `() {
        val stubbedClient = mockk<BehandleArbeidOgAktivitetOppgaveV1>()
        every { stubbedClient.bestillOppgave(any()) } returns WSBestillOppgaveResponse().withArenaSakId("123")

        val client = ArenaOppgaveClient(stubbedClient)

        val actual = client.bestillOppgave("123456789", "abcbscb")

        actual shouldBe "123"
    }

    @Test
    fun ``


}
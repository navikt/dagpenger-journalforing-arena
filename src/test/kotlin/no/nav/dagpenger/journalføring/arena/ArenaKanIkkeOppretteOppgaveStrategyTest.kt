package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import org.junit.jupiter.api.Test

class ArenaKanIkkeOppretteOppgaveStrategyTest {
    @Test
    fun `skal ikke opprette oppgave når det er aktive saker`() {
        val fakta = Fakta(
                naturligIdent = "12345678",
                enhetId = "1234",
                arenaSaker = listOf(ArenaSak(124, ArenaSakStatus.Aktiv)),
                journalpostId = "987",
                dokumentTitler = listOf(""),
                registrertDato = "2019-12-24T23-59-01")

        val strategy = ArenaKanIkkeOppretteOppgaveStrategy()
        strategy.canHandle(fakta) shouldBe true

        val resultat = strategy.handle(fakta)
        resultat shouldBe null
    }

    @Test
    fun `skal ikke håndtere fakta hvor det ikke er aktive saker`() {
        val fakta = Fakta(
                naturligIdent = "12345678",
                enhetId = "1234",
                arenaSaker = listOf(ArenaSak(124, ArenaSakStatus.Inaktiv)),
                journalpostId = "987",
                dokumentTitler = listOf(""),
                registrertDato = "2019-12-24T23-59-01")

        val strategy = ArenaKanIkkeOppretteOppgaveStrategy()
        strategy.canHandle(fakta) shouldBe false
    }
}
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
            registrertDato = "2019-11-22T12:01:57",
            dokumentTitler = listOf("Hoved", "vedlegg")
        )

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
            registrertDato = "2019-11-22T12:01:57",
            dokumentTitler = listOf("Hoved", "vedlegg")
            )

        val strategy = ArenaKanIkkeOppretteOppgaveStrategy()
        strategy.canHandle(fakta) shouldBe false
    }
}
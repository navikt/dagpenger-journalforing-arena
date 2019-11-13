package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import org.junit.jupiter.api.Test

class ArenaKanIkkeOppretteOppgaveStrategyTest {
    @Test
    fun `skal ikke opprette oppgave når det er aktive saker`() {
        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, "AKTIV")))

        val strategy = ArenaKanIkkeOppretteOppgaveStrategy()
        strategy.canHandle(fakta) shouldBe true

        val resultat = strategy.handle(fakta)
        resultat.arenaSakId shouldBe null
        resultat.opprettet shouldBe false
    }

    @Test
    fun `skal ikke håndtere fakta hvor det ikke er aktive saker`() {
        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, "INAKTIV")))

        val strategy = ArenaKanIkkeOppretteOppgaveStrategy()
        strategy.canHandle(fakta) shouldBe false

    }
}
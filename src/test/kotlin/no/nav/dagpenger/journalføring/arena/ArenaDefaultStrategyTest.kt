package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakId
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import org.junit.jupiter.api.Test

internal class ArenaDefaultStrategyTest {
    @Test
    fun `skal returnere ArenaResultat uten saksId og ikke opprette oppgave om den ikke finner passende strategi`() {
        val arenaResultat = ArenaDefaultStrategy(listOf()).handle(Fakta("1010101", "NAV", listOf(), "987"))
        arenaResultat shouldBe null
    }

    @Test
    fun `skal bestille oppgave når Fakta inneholder liste uten aktiv sak`() {

        val arenaResultat = ArenaDefaultStrategy(listOf(SuksessBestillOppgaveStrategi(), ArenaKanIkkeOppretteOppgaveStrategy())).handle(
            Fakta("1010101", "NAV", listOf(ArenaSak(123, ArenaSakStatus.Inaktiv)), "987"))
        arenaResultat?.id shouldBe "123"
    }

    internal class SuksessBestillOppgaveStrategi : ArenaStrategy {
        override fun canHandle(fakta: Fakta): Boolean {
            return true
        }

        override fun handle(fakta: Fakta): ArenaSakId? {
            return ArenaSakId(id = "123")
        }
    }
}

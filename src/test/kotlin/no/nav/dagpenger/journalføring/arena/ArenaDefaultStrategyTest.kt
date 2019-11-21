package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import org.junit.jupiter.api.Test

internal class ArenaDefaultStrategyTest {
    @Test
    fun `skal returnere ArenaResultat uten saksId og ikke opprette oppgave om den ikke finner passende strategi`() {
        val arenaResultat = ArenaDefaultStrategy(listOf()).handle(Fakta("1010101", "NAV", listOf()))
        arenaResultat.arenaSakId shouldBe null
        arenaResultat.opprettet shouldBe false
    }

    @Test
    fun `skal bestille oppgave når Fakta inneholder liste uten aktiv sak`() {

        val arenaResultat = ArenaDefaultStrategy(listOf(SuksessBestillOppgaveStrategi(), ArenaKanIkkeOppretteOppgaveStrategy())).handle(
            Fakta("1010101", "NAV", listOf(ArenaSak(123, ArenaSakStatus.Inaktiv))))
        arenaResultat.arenaSakId shouldBe "123"
        arenaResultat.opprettet shouldBe true
    }

    internal class SuksessBestillOppgaveStrategi : ArenaStrategy {
        override fun canHandle(fakta: Fakta): Boolean {
            return true
        }

        override fun handle(fakta: Fakta): ArenaResultat {
            return ArenaResultat("123", true)
        }
    }
}

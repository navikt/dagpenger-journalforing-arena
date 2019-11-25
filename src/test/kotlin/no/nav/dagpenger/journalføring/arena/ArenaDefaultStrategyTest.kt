package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakId
import org.junit.jupiter.api.Test

internal class ArenaDefaultStrategyTest {
    @Test
    fun `skal returnere ArenaResultat uten saksId og ikke opprette oppgave om den ikke finner passende strategi`() {
        val fakta = Fakta(
            naturligIdent = "1010101",
            enhetId = "NAV",
            arenaSaker = listOf(),
            registrertDato = "2019-11-22T12:01:57",
            dokumentTitler = listOf("Hoved", "vedlegg")
        )
        val arenaResultat = ArenaDefaultStrategy(listOf()).handle(fakta)
        arenaResultat shouldBe null
    }

    @Test
    fun `skal bestille oppgave når Fakta inneholder liste uten aktiv sak`() {
        val fakta = Fakta(
            naturligIdent = "1010101",
            enhetId = "NAV",
            arenaSaker = listOf(),
            registrertDato = "2019-11-22T12:01:57",
            dokumentTitler = listOf("Hoved", "vedlegg")
        )

        val arenaResultat =
            ArenaDefaultStrategy(listOf(SuksessBestillOppgaveStrategi(), ArenaKanIkkeOppretteOppgaveStrategy())).handle(
                fakta
            )
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

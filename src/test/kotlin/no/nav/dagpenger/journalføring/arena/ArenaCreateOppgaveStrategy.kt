package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import org.junit.jupiter.api.Test

class ArenaCreateOppgaveStrategyTest {

    @Test
    fun `Skal opprette oppgave når det ikke er aktive saker`() {

        val arenaOppgaveClient: ArenaClient = mockk()
        every {
            arenaOppgaveClient.bestillOppgave("12345678", "1234")
        } returns "1234"

        val unleashMock: Unleash = mockk()
        every {
            unleashMock.isEnabled("dp-arena.bestillOppgaveLOCAL", false)
        } returns true

        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, "INAKT")))

        val strategy = ArenaCreateOppgaveStrategy(arenaOppgaveClient, unleashMock, Profile.LOCAL)
        strategy.canHandle(fakta) shouldBe true

        val resultat = strategy.handle(fakta)
        resultat.arenaSakId shouldNotBe null
        resultat.opprettet shouldBe true
    }

    @Test
    fun `Skal ikke opprette oppgave når det er aktive saker og toggle er på`() {
        val arenaOppgaveClient: ArenaClient = mockk()
        every {
            arenaOppgaveClient.bestillOppgave("12345678", "1234")
        } returns "1234"

        val unleashMock: Unleash = mockk()
        every {
            unleashMock.isEnabled("dp-arena.bestillOppgaveLOCAL", false)
        } returns true

        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, "AKTIV")))

        val strategy = ArenaCreateOppgaveStrategy(arenaOppgaveClient, unleashMock, Profile.LOCAL)
        strategy.canHandle(fakta) shouldBe false
    }

    @Test
    fun `Skal ikke opprette oppgave når det er ikke aktive saker og men toggle er av`() {
        val arenaOppgaveClient: ArenaClient = mockk()
        every {
            arenaOppgaveClient.bestillOppgave("12345678", "1234")
        } returns "1234"

        val unleashMock: Unleash = mockk()
        every {
            unleashMock.isEnabled("dp-arena.bestillOppgaveLOCAL", false)
        } returns false

        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, "INAKT")))

        val strategy = ArenaCreateOppgaveStrategy(arenaOppgaveClient, unleashMock, Profile.LOCAL)
        strategy.canHandle(fakta) shouldBe false
    }
}
package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import no.nav.dagpenger.journalføring.arena.adapter.BestillOppgaveArenaException
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BestillOppgavePersonErInaktiv
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
            unleashMock.isEnabled("dp-arena.bestillOppgave", false)
        } returns true

        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, ArenaSakStatus.Inaktiv)))

        val strategy = ArenaCreateOppgaveStrategy(arenaOppgaveClient, unleashMock)
        strategy.canHandle(fakta) shouldBe true

        val resultat = strategy.handle(fakta)
        resultat shouldNotBe null
    }

    @Test
    fun `Skal ikke opprette oppgave når det er aktive saker og toggle er på`() {
        val arenaOppgaveClient: ArenaClient = mockk()
        every {
            arenaOppgaveClient.bestillOppgave("12345678", "1234")
        } returns "1234"

        val unleashMock: Unleash = mockk()
        every {
            unleashMock.isEnabled("dp-arena.bestillOppgave", false)
        } returns true

        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, ArenaSakStatus.Aktiv)))

        val strategy = ArenaCreateOppgaveStrategy(arenaOppgaveClient, unleashMock)
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
            unleashMock.isEnabled("dp-arena.bestillOppgave", false)
        } returns false

        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, ArenaSakStatus.Inaktiv)))

        val strategy = ArenaCreateOppgaveStrategy(arenaOppgaveClient, unleashMock)
        strategy.canHandle(fakta) shouldBe false
    }

    @Test
    fun `Skal opprette manuell oppgave når søker ikke er registrert som arbeidssøker i Arena (kaster BestillOppgavePersonErInaktiv) `() {
        val arenaOppgaveClient: ArenaClient = mockk()
        every {
            arenaOppgaveClient.bestillOppgave("12345678", "1234")
        } throws BestillOppgaveArenaException(BestillOppgavePersonErInaktiv())

        val unleashMock: Unleash = mockk()
        every {
            unleashMock.isEnabled("dp-arena.bestillOppgave", false)
        } returns true

        val fakta = Fakta(
            naturligIdent = "12345678",
            enhetId = "1234",
            arenaSaker = listOf(ArenaSak(124, ArenaSakStatus.Inaktiv)))

        val strategy = ArenaCreateOppgaveStrategy(arenaOppgaveClient, unleashMock)
        strategy.handle(fakta) shouldBe null
    }
}
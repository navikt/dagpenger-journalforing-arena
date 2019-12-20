package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakId
import org.junit.jupiter.api.Test

internal class JournalføringArenaPacketHandlingTest {
    @Test
    fun `skal legge på arenaSakId på pakken når arenaSakId blir gitt fra strategien`() {
        val strategy = mockk<ArenaStrategy>()

        every {strategy.handle(any())} returns ArenaSakId(id = "123")

        val journalføringArena = JournalføringArena(strategy, mockk(relaxed = true))

        val packet = Packet().apply {
            putValue("behandlendeEnhet", "1234")
            putValue("naturligIdent", "12345678")
            putValue("journalpostId", "666")
            putValue("dokumenter", dokumentAdapter.toJsonValue(listOf(Dokument("Søknad 1")))!!)
            putValue("datoRegistrert", "2019")
        }

        journalføringArena.handlePacket(packet).apply {
            this.getStringValue("arenaSakId") shouldBe "123"
            this.getBoolean("arenaSakOpprettet") shouldBe true
        }
    }
}
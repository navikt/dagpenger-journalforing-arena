package no.nav.dagpenger.journalføring.arena

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.junit.jupiter.api.Test

class DokumentAdapterTest {
    data class DokumentInfo(val tittel: String, val brevkode: String)
    @Test
    fun `Skal greie å konvertere dokumentInfo med flere felter til Dokument med tittel`() {

        val json = """[{"tittel": "en", "brevkode": "123"}, {"tittel": "to"}]""".trimIndent()

        val konverterteDokumenter = dokumentAdapter.fromJson(json)

        konverterteDokumenter shouldNotBe null
        konverterteDokumenter?.first()?.tittel shouldBe "en"
        konverterteDokumenter?.last()?.tittel shouldBe "to"
    }
}
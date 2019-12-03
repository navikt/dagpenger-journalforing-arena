package no.nav.dagpenger.journalføring.arena

import com.squareup.moshi.Types
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import no.nav.dagpenger.events.moshiInstance
import org.junit.jupiter.api.Test

class DokumentAdapterTest {
    data class DokumentInfo(val tittel: String, val brevkode: String)
    @Test
    fun `Skal greie å konvertere dokumentInfo med flere felter til Dokument med tittel`() {
        val dokumenter = listOf(DokumentInfo("en", "123"), DokumentInfo("to", "123"))
        val dokumentInfoAdapter = moshiInstance.adapter<List<DokumentInfo>>(
            Types.newParameterizedType(
                List::class.java,
                DokumentInfo::class.java
            )
        )

        val json = dokumentInfoAdapter.toJsonValue(dokumenter)

        val konverterteDokumenter = dokumentAdapter.fromJsonValue(json)

        konverterteDokumenter shouldNotBe null
        konverterteDokumenter?.first()?.tittel shouldBe "en"
        konverterteDokumenter?.last()?.tittel shouldBe "to"
    }
}
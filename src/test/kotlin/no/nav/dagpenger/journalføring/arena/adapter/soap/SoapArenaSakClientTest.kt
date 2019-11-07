package no.nav.dagpenger.journalføring.arena.adapter.soap

import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.arena.services.lib.sakvedtak.SaksInfo
import no.nav.arena.services.lib.sakvedtak.SaksInfoListe
import no.nav.arena.services.sakvedtakservice.SakVedtakPortType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.xml.ws.Holder

internal class SoapArenaSakClientTest {


    @Test
    fun `Skal kunne hente arena saker basert på bruker id` (){


        val sakVedtakPortType: SakVedtakPortType = mockk()

        every { sakVedtakPortType.hentSaksInfoListeV2(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            Holder(SaksInfoListe().withSaksInfo(listOf(
                SaksInfo().withSaksId("123")
            ))) //
        ) } returns Unit

        val client = SoapArenaSakClient(sakVedtakPortType)

        val saker = client.hentArenaSaker(
            "1234"
        )

        saker.size shouldNotBe 0
    }


}
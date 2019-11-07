package no.nav.dagpenger.journalføring.arena.adapter.soap

import no.nav.arena.services.lib.sakvedtak.SaksInfo
import no.nav.arena.services.lib.sakvedtak.SaksInfoListe
import no.nav.arena.services.sakvedtakservice.Bruker
import no.nav.arena.services.sakvedtakservice.SakVedtakPortType
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakClient
import javax.xml.ws.Holder

class SoapArenaSakClient(private val sakVedtakPortType: SakVedtakPortType) : ArenaSakClient {

    override fun hentArenaSaker(naturligIdent: String): List<SaksInfo> {

        val resultat = Holder<SaksInfoListe>()
        val bruker = Bruker().withBrukerId(naturligIdent).withBrukertypeKode("PERSON")
        sakVedtakPortType.hentSaksInfoListeV2(
            Holder(bruker),
            null,
            null,
            null,
            "DAG",
            null,
            resultat
        )

        return resultat.value.saksInfo
    }
}
package no.nav.dagpenger.journalføring.arena.adapter.soap.arena

import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClientException
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSOppgave
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSOppgavetype
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSPerson
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSPrioritet
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSTema
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveRequest
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveResponse
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

class SoapArenaClient(private val oppgaveV1: BehandleArbeidOgAktivitetOppgaveV1, private val ytelseskontraktV3: YtelseskontraktV3) : ArenaClient {
    override fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String {
        val soapRequest = WSBestillOppgaveRequest()

        soapRequest.oppgavetype = WSOppgavetype().apply { value = "STARTVEDTAK" }

        val today = ZonedDateTime.now().toInstant().atZone(ZoneId.of("Europe/Oslo"))

        soapRequest.oppgave = WSOppgave().apply {
            tema = WSTema().apply { value = "DAG" }
            bruker = WSPerson().apply { ident = naturligIdent }
            this.behandlendeEnhetId = behandlendeEnhetId
            prioritet = WSPrioritet().apply {
                this.kodeRef = "HOY"
            }
            frist = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(today))
        }

        val response: WSBestillOppgaveResponse = try {
            oppgaveV1.bestillOppgave(soapRequest)
        } catch (e: Exception) {
            throw ArenaClientException(e)
            // @todo Håndtere BestillOppgaveSikkerhetsbegrensning, BestillOppgaveOrganisasjonIkkeFunnet, BestillOppgavePersonErInaktiv, BestillOppgaveSakIkkeOpprettet, BestillOppgavePersonIkkeFunnet, BestillOppgaveUgyldigInput;
        }

        return response.arenaSakId
    }

    override fun hentArenaSaker(naturligIdent: String): List<ArenaSak> {
        val request = WSHentYtelseskontraktListeRequest().withPersonidentifikator(naturligIdent)
        val response = ytelseskontraktV3.hentYtelseskontraktListe(request)
        return response.ytelseskontraktListe.filter { it.ytelsestype == "DAGP" }.map { ArenaSak(it.fagsystemSakId, it.status) }
    }
}

data class ArenaSak(val fagsystemSakId: Int, val status: String)

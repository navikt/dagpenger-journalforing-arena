package no.nav.dagpenger.journalføring.arena.adapter.soap.arena

import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaOppgaveClientException
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSOppgave
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSOppgavetype
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSPerson
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSPrioritet
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSTema
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveRequest
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveResponse
import no.nav.virksomhet.gjennomforing.sak.arbeidogaktivitet.v1.Sak
import no.nav.virksomhet.tjenester.sak.arbeidogaktivitet.v1.ArbeidOgAktivitet
import no.nav.virksomhet.tjenester.sak.meldinger.v1.WSBruker
import no.nav.virksomhet.tjenester.sak.meldinger.v1.WSHentSakListeRequest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

class SoapArenaClient(
    private val oppgaveV1: BehandleArbeidOgAktivitetOppgaveV1,
    private val arbeidOgAktivitet: ArbeidOgAktivitet
) : ArenaClient {
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
            throw ArenaOppgaveClientException(e)
            // @todo Håndtere BestillOppgaveSikkerhetsbegrensning, BestillOppgaveOrganisasjonIkkeFunnet, BestillOppgavePersonErInaktiv, BestillOppgaveSakIkkeOpprettet, BestillOppgavePersonIkkeFunnet, BestillOppgaveUgyldigInput;
        }

        return response.arenaSakId
    }

    override fun hentArenaSaker(naturligIdent: String): List<Sak> {

        // val resultat = Holder<SaksInfoListe>()
        val bruker = WSBruker().withBruker(naturligIdent).withBrukertypeKode("PERSON")
        val request = WSHentSakListeRequest().withBruker(bruker).withFagomradeKode("DAG")

        val resultat = arbeidOgAktivitet.hentSakListe(request)
        // arenaSakVedtakService.hentSaksInfoListeV2(
        //     Holder(bruker),
        //     null,
        //     null,
        //     null,
        //     "DAG",
        //     null,
        //     resultat
        // )

        return resultat.sakListe
    }
}
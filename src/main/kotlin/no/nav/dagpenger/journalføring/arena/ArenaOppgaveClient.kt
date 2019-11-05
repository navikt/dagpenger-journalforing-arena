package no.nav.dagpenger.journalføring.arena

import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSOppgave
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSOppgavetype
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSPerson
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.informasjon.WSTema
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveRequest
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.meldinger.WSBestillOppgaveResponse

class ArenaOppgaveClient(val oppgaveV1: BehandleArbeidOgAktivitetOppgaveV1) {
    fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String {

        val soapRequest = WSBestillOppgaveRequest()

        soapRequest.oppgavetype = WSOppgavetype().apply { value = "STARTVEDTAK" }
        soapRequest.oppgave = WSOppgave().apply {
            tema = WSTema().apply { value = "DAG" }
            bruker = WSPerson().apply { ident = naturligIdent }
            this.behandlendeEnhetId = behandlendeEnhetId
        }

        val response: WSBestillOppgaveResponse = oppgaveV1.bestillOppgave(soapRequest)

        return response.arenaSakId

    }
}
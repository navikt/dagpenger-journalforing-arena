package no.nav.dagpenger.journalføring.arena.adapter

import no.nav.virksomhet.gjennomforing.sak.arbeidogaktivitet.v1.Sak

interface ArenaClient {
    fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String
    fun hentArenaSaker(naturligIdent: String): List<Sak>
}

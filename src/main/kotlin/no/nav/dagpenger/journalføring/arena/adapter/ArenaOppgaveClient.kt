package no.nav.dagpenger.journalføring.arena.adapter

interface ArenaOppgaveClient {
    fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String
}

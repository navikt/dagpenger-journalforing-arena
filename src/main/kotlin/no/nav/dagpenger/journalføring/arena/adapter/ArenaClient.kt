package no.nav.dagpenger.journalføring.arena.adapter

interface ArenaClient {
    fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String
    fun hentArenaSaker(naturligIdent: String): List<ArenaSak>
}

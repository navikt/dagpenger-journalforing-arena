package no.nav.dagpenger.journalføring.arena.adapter

import no.nav.dagpenger.journalføring.arena.adapter.soap.arena.ArenaSak

interface ArenaClient {
    fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String
    fun hentArenaSaker(naturligIdent: String): List<ArenaSak>
}

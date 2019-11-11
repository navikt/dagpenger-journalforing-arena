package no.nav.dagpenger.journalføring.arena.adapter

import no.nav.arena.services.lib.sakvedtak.SaksInfo

interface ArenaClient {
    fun bestillOppgave(naturligIdent: String, behandlendeEnhetId: String): String
    fun hentArenaSaker(naturligIdent: String): List<SaksInfo>
}

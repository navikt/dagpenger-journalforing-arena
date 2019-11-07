package no.nav.dagpenger.journalføring.arena.adapter

import no.nav.arena.services.lib.sakvedtak.SaksInfo

interface ArenaSakClient {
    fun hentArenaSaker(naturligIdent: String): List<SaksInfo>
}
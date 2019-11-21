package no.nav.dagpenger.journalføring.arena.adapter

data class ArenaSak(val fagsystemSakId: Int, val status: ArenaSakStatus)

enum class ArenaSakStatus {
    Aktiv, Inaktiv, Lukket
}

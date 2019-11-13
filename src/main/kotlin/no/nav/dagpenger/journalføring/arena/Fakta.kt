package no.nav.dagpenger.journalføring.arena

import no.nav.dagpenger.journalføring.arena.adapter.ArenaSak

data class Fakta(
    val naturligIdent: String,
    val enhetId: String,
    val arenaSaker: List<ArenaSak>
)

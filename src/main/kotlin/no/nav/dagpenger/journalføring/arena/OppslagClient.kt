package no.nav.dagpenger.journalføring.arena

interface OppslagClient {
    fun createOppgave(request: CreateArenaOppgaveRequest): String
    fun getSaker(request: GetArenaSakerRequest): List<ArenaSak>
}
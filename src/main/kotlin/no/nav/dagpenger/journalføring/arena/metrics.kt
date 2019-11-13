package no.nav.dagpenger.journalføring.arena

import io.prometheus.client.Counter

val arenaOppgaveTeller = Counter
    .build()
    .name("oppgave_opprettet_arena")
    .help("Antall oppgaver opprettet i arena")
    .register()

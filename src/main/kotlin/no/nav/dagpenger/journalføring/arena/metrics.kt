package no.nav.dagpenger.journalføring.arena

import io.prometheus.client.Counter

val automatiskJournalførtTeller = Counter
    .build()
    .name("automatisk_journalfort_arena")
    .help("Antall søknader som er automatisk journalført i Arena")
    .labelNames("opprettet", "grunn")
    .register()

val automatiskJournalførtJaTeller = automatiskJournalførtTeller.labels("true", "arena_ok")
fun automatiskJournalførtNeiTeller(reason: String) = automatiskJournalførtTeller.labels("false", reason).inc()

val antallDagpengerSaker = Counter
    .build()
    .name("dagpenger_saker")
    .help("Antall dagpengesaker basert på status")
    .labelNames("status")
    .register()

val aktiveDagpengeSakTeller = antallDagpengerSaker.labels("aktiv")
val avsluttetDagpengeSakTeller = antallDagpengerSaker.labels("avsluttet")
val inaktivDagpengeSakTeller = antallDagpengerSaker.labels("inaktiv")

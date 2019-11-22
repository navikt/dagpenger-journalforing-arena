package no.nav.dagpenger.journalføring.arena.adapter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun createArenaOppgaveBeskrivelse(dokumentTitler: List<String>, registrertDato: String): String {
    LocalDateTime                             // Represent a date with time-of-day but lacking offset-from-UTC or time zone. As such, this does *not* represent a moment, is *not* a point on the timeline.
        .parse( registrertDato )      // Parse an input string in standard ISO 8601 format. Returns a `LocalDateTime` object.
        .toLocalDate()                            // Extract the date-only portion without the time-of-day. Still no time zone or offset-from-UTC. Returns a `LocalDate` object.
        .format(                                  // Generate text representing the value of that `LocalDate` object.
            DateTimeFormatter                     // Define a pattern to use in generating text.
                .ofPattern(
                    "dd.MM.yyyy"
                )
        )
    return "Hoveddokument: ${dokumentTitler.first()}\n" +
        dokumentTitler.drop(1).map { "- $it\n" }. +
        "Registrert dato: ${registrertDato}\n" +
        "Dokumentet er skannet inn og journalført automatisk. Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\". "
}
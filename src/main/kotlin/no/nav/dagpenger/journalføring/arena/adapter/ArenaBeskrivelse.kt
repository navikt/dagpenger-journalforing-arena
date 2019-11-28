package no.nav.dagpenger.journalføring.arena.adapter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun createArenaOppgaveBeskrivelse(dokumentTitler: List<String>, registrertDato: String): String {
    val hovedDokument = dokumentTitler.first()
    val vedlegg = dokumentTitler.drop(1)

    val formatertVedlegg =
            if (vedlegg.isNotEmpty()) {
                vedlegg.joinToString(prefix = "- ", separator = "\n- ", postfix = "\n")
            } else { "" }

    val formatertDato =
            LocalDateTime
                    .parse(registrertDato)
                    .toLocalDate()
                    .format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    )

    return "Hoveddokument: ${hovedDokument}\n" +
            formatertVedlegg +
            "Registrert dato: ${formatertDato}\n" +
            "Dokumentet er skannet inn og journalført automatisk av digitale dagpenger. Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\". "
}

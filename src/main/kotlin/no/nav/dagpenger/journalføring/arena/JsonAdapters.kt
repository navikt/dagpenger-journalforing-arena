package no.nav.dagpenger.journalføring.arena

import com.squareup.moshi.Types
import no.nav.dagpenger.events.moshiInstance

val dokumentAdapter = moshiInstance.adapter<List<Dokument>>(
    Types.newParameterizedType(
        List::class.java,
        Dokument::class.java
    )
)

data class Dokument(
    val tittel: String
)
package no.nav.dagpenger.journalføring.arena

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import no.nav.dagpenger.events.moshiInstance

data class Behandlendeenhet(val enhetId: String, val enhetNavn: String)

val behandlendeenhetAdapter: JsonAdapter<List<Behandlendeenhet>> = moshiInstance.adapter(
    Types.newParameterizedType(
        List::class.java,
        Behandlendeenhet::class.java
    )
)
package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus

private val logger = KotlinLogging.logger {}

interface ArenaStrategy {
    fun canHandle(fakta: Fakta): Boolean
    fun handle(fakta: Fakta): ArenaResultat
}

class ArenaDefaultStrategy(val strategies: List<ArenaStrategy>) : ArenaStrategy {
    override fun canHandle(fakta: Fakta) = true

    override fun handle(fakta: Fakta) =
        strategies.filter { it.canHandle(fakta) }
            .map { it.handle(fakta) }.firstOrNull() ?: default()

    private fun default(): ArenaResultat {
        logger.info { "Strategy ${this.javaClass.name} handling this" }
        return ArenaResultat(null, false)
    }
}

class ArenaCreateOppgaveStrategy(
    private val arenaClient: ArenaClient,
    private val unleash: Unleash,
    private val profile: Profile
) : ArenaStrategy {

    override fun canHandle(fakta: Fakta): Boolean {
        return fakta.arenaSaker.none { it.status == ArenaSakStatus.Aktiv } && unleash.isEnabled(
            "dp-arena.bestillOppgave${profile.name}",
            false
        )
    }

    override fun handle(fakta: Fakta): ArenaResultat {
        val arenaSakId = arenaClient.bestillOppgave(fakta.naturligIdent, fakta.enhetId)
        logger.info { "Strategy ${this.javaClass.name} handling this" }
        return ArenaResultat(arenaSakId, true)
    }
}

class ArenaKanIkkeOppretteOppgaveStrategy : ArenaStrategy {
    override fun canHandle(fakta: Fakta): Boolean {
        return fakta.arenaSaker.any { it.status == ArenaSakStatus.Aktiv }
    }

    override fun handle(fakta: Fakta): ArenaResultat {
        logger.info { "Strategy ${this.javaClass.name} handling this" }
        return ArenaResultat(null, false)
    }
}

data class ArenaResultat(val arenaSakId: String?, val opprettet: Boolean)

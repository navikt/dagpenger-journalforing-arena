package no.nav.dagpenger.journalføring.arena

import no.finn.unleash.Unleash
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakId
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import no.nav.dagpenger.journalføring.arena.adapter.BestillOppgaveArenaException
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BestillOppgavePersonErInaktiv

interface ArenaStrategy {
    fun canHandle(fakta: Fakta): Boolean
    fun handle(fakta: Fakta): ArenaSakId?
}

class ArenaDefaultStrategy(private val strategies: List<ArenaStrategy>) : ArenaStrategy {
    override fun canHandle(fakta: Fakta) = true

    override fun handle(fakta: Fakta): ArenaSakId? =
        strategies.filter { it.canHandle(fakta) }
            .map { it.handle(fakta) }.firstOrNull() ?: default()

    private fun default(): ArenaSakId? {
        automatiskJournalførtNeiTeller.inc()
        return null
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

    override fun handle(fakta: Fakta): ArenaSakId? {
        val arenaSakId = try {
            arenaClient.bestillOppgave(fakta.naturligIdent, fakta.enhetId)
        } catch (e: BestillOppgaveArenaException) {
            return when (e.cause) {
                is BestillOppgavePersonErInaktiv -> {

                    null
                }
                else -> throw e
            }
        }
        automatiskJournalførtJaTeller.inc()
        return ArenaSakId(id = arenaSakId)
    }
}

class ArenaKanIkkeOppretteOppgaveStrategy : ArenaStrategy {
    override fun canHandle(fakta: Fakta): Boolean {
        return fakta.arenaSaker.any { it.status == ArenaSakStatus.Aktiv }
    }

    override fun handle(fakta: Fakta): ArenaSakId? {
        automatiskJournalførtNeiTeller.inc()
        return null
    }
}

data class ArenaResultat(val arenaSakId: String?, val opprettet: Boolean)

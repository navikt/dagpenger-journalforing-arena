package no.nav.dagpenger.journalføring.arena

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.dagpenger.journalføring.arena.adapter.ArenaClient
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakId
import no.nav.dagpenger.journalføring.arena.adapter.ArenaSakStatus
import no.nav.dagpenger.journalføring.arena.adapter.BestillOppgaveArenaException
import no.nav.dagpenger.journalføring.arena.adapter.createArenaOppgaveBeskrivelse
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BestillOppgavePersonErInaktiv
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BestillOppgavePersonIkkeFunnet

private val logger = KotlinLogging.logger {}

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
        automatiskJournalførtNeiTeller("ukjent_default")
        return null
    }
}

class ArenaCreateOppgaveStrategy(
    private val arenaClient: ArenaClient,
    private val unleash: Unleash
) : ArenaStrategy {

    override fun canHandle(fakta: Fakta): Boolean {
        return fakta.arenaSaker.none { it.status == ArenaSakStatus.Aktiv }
    }

    override fun handle(fakta: Fakta): ArenaSakId? {
        if (unleash.isEnabled("dp-arena.bestillOppgave", false)) {
            val beskrivelse = createArenaOppgaveBeskrivelse(fakta.dokumentTitler, fakta.registrertDato)
            val arenaSakId = try {
                arenaClient.bestillOppgave(fakta.naturligIdent, fakta.enhetId, beskrivelse)
            } catch (e: BestillOppgaveArenaException) {
                automatiskJournalførtNeiTeller(e.cause?.javaClass?.simpleName ?: "ukjent")
                return when (e.cause) {
                    is BestillOppgavePersonErInaktiv -> {
                        logger.warn { "Kan ikke bestille oppgave for journalpost ${fakta.journalpostId}. Person ikke arbeidssøker " }
                        null
                    }
                    is BestillOppgavePersonIkkeFunnet -> {
                        logger.warn { "Kan ikke bestille oppgave for journalpost ${fakta.journalpostId}. Person ikke funnet i arena " }
                        null
                    }
                    else -> {
                        logger.warn { "Kan ikke bestille oppgave for journalpost ${fakta.journalpostId}. Ukjent feil. " }
                        throw e
                    }
                }
            }
            return ArenaSakId(id = arenaSakId)
        } else {
            automatiskJournalførtNeiTeller("feature_toggle_off")
            return null
        }
    }
}

class ArenaKanIkkeOppretteOppgaveStrategy : ArenaStrategy {
    override fun canHandle(fakta: Fakta): Boolean {
        return fakta.arenaSaker.any { it.status == ArenaSakStatus.Aktiv }
    }

    override fun handle(fakta: Fakta): ArenaSakId? {
        automatiskJournalførtNeiTeller("aktiv_sak")
        return null
    }
}

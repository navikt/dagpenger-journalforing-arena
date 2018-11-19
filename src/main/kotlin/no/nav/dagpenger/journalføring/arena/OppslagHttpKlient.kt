package no.nav.dagpenger.journalføring.arena

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson

class OppslagHttpClient(private val oppslagUrl: String) {

    fun createOppgave(createOppgaveRequest: CreateArenaOppgaveRequest): String {

        val requestJson = Gson().toJson(createOppgaveRequest).toString()

        val (_, response, result) = with(
            "${oppslagUrl}arena/createoppgave".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body(requestJson)
        ) {
            responseObject<CreateArenaOppgaveResponse>()
        }
        return when (result) {
            is Result.Failure -> throw OppslagException(
                response.statusCode, response.responseMessage, result.getException()
            )
            is Result.Success -> result.get().sakId
        }
    }

    fun findSak(fødselsnummer: String): String {

        val requestJson = Gson().toJson(FindArenaSakRequest(fødselsnummer, "PERSON", "DAG")).toString()

        val (_, response, result) = with(
            "${oppslagUrl}arena/findsak".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body(requestJson)
        ) {
            responseObject<FindArenaSakResponse>()
        }
        return when (result) {
            is Result.Failure -> throw OppslagException(
                response.statusCode, response.responseMessage, result.getException()
            )
            is Result.Success -> result.get().sakId
        }
    }
}

data class CreateArenaOppgaveRequest(
    val behandlendeEnhetId: String,
    val fødselsnummer: String,
    val sakId: String?,
    val oppgaveType: String,
    val tvingNySak: Boolean,
    val tema: String = "DAG",
    val prioritet: String = "HOY"
)

data class CreateArenaOppgaveResponse(
    val sakId: String
)

data class FindArenaSakRequest(
    val fødselsnummer: String,
    val brukerType: String,
    val tema: String = "DAG"
)

data class FindArenaSakResponse(
    val sakId: String
)

class OppslagException(val statusCode: Int, override val message: String, override val cause: Throwable) :
    RuntimeException("$statusCode: $message", cause)

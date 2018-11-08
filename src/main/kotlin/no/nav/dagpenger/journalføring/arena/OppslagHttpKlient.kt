package no.nav.dagpenger.journalføring.arena

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson

class OppslagHttpClient(private val oppslagUrl: String) {

    fun createSak(behandlendeEnhetId: String, fødselsnummer: String): String {

        val requestJson = Gson().toJson(CreateArenaSakRequest(behandlendeEnhetId, fødselsnummer)).toString()

        val (_, response, result) = with(
            "${oppslagUrl}arena/createsak".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body(requestJson)
        ) {
            responseObject<CreateArenaSakResponse>()
        }
        return when (result) {
            is Result.Failure -> throw OppslagException(
                response.statusCode, response.responseMessage, result.getException()
            )
            is Result.Success -> result.get().sakId
        }
    }

    fun createOppgave(behandlendeEnhetId: String, fødselsnummer: String, sakId: String): String {

        val requestJson = Gson().toJson(CreateArenaOppgaveRequest(behandlendeEnhetId, fødselsnummer, sakId)).toString()

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
            is Result.Success -> result.get().oppgaveId
        }
    }

    fun findSak(fødselsnummer: String): String {

        val requestJson = Gson().toJson(FindArenaSakRequest(fødselsnummer)).toString()

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

data class CreateArenaSakRequest(
    val behandlendeEnhetId: String,
    val fødselsnummer: String
)

data class CreateArenaSakResponse(
    val sakId: String
)

data class CreateArenaOppgaveRequest(
    val behandlendeEnhetId: String,
    val fødselsnummer: String,
    val sakId: String
)

data class CreateArenaOppgaveResponse(
    val oppgaveId: String
)

data class FindArenaSakRequest(
    val fødselsnummer: String
)

data class FindArenaSakResponse(
    val sakId: String
)

class OppslagException(val statusCode: Int, override val message: String, override val cause: Throwable) :
    RuntimeException(message, cause)

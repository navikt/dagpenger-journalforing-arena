package no.nav.dagpenger.journalføring.arena

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import java.util.Date
import javax.xml.datatype.XMLGregorianCalendar

class OppslagHttpClient(private val oppslagUrl: String) {

    fun createOppgave(request: CreateArenaOppgaveRequest): String {

        val requestJson = Gson().toJson(request).toString()

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

    fun getSaker(request: GetArenaSakerRequest): List<ArenaSak> {

        val requestJson = Gson().toJson(request).toString()

        val (_, response, result) = with(
            "${oppslagUrl}arena/getsaker".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body(requestJson)
        ) {
            responseObject<GetArenaSakerResponse>()
        }
        return when (result) {
            is Result.Failure -> throw OppslagException(
                response.statusCode, response.responseMessage, result.getException()
            )
            is Result.Success -> result.get().saker
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

data class GetArenaSakerRequest(
    val fødselsnummer: String,
    val brukerType: String,
    val tema: String = "DAG",
    val includeInactive: Boolean = false
)

data class GetArenaSakerResponse(
    val saker: List<ArenaSak>
)

data class ArenaSak(val sakId: String, val sakstatus: String, val sakOpprettet: Date)

class OppslagException(val statusCode: Int, override val message: String, override val cause: Throwable) :
    RuntimeException("$statusCode: $message", cause)

package no.nav.dagpenger.journalføring.arena

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class OppslagHttpClientTest {

    @Rule
    @JvmField
    var wireMockRule = WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort())

    @Test
    fun `create oppgave`() {

        val createArenaOppgaveRequest =
            CreateArenaOppgaveRequest("123", "12345678912", null, "BEHENVPERSON", true)

        val createArenaOppgaveRequestjson = """
        {
            "behandlendeEnhetId": "123",
            "fødselsnummer": "12345678912",
            "oppgaveType": "BEHENVPERSON",
            "tema": "DAG",
            "prioritet": "HOY",
            "tvingNySak": true
        }"""

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/arena/createoppgave"))
                .withRequestBody(EqualToJsonPattern(createArenaOppgaveRequestjson, true, true))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "sakId": "1458"
                            }
                        """.trimIndent()
                        )
                )
        )

        val sakId = OppslagHttpClient(wireMockRule.url("")).createOppgave(createArenaOppgaveRequest)
        assertEquals("1458", sakId)
    }

    @Test(expected = OppslagException::class)
    fun `create oppgave feiler`() {

        val createArenaOppgaveRequest =
            CreateArenaOppgaveRequest("123", "12345678912", null, "BEHENVPERSON", true)

        val createArenaOppgaveRequestjson = """
        {
            "behandlendeEnhetId": "123",
            "fødselsnummer": "12345678912",
            "oppgaveType": "BEHENVPERSON",
            "tema": "DAG",
            "prioritet": "HOY",
            "tvingNySak": true
        }"""

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/arena/createoppgave"))
                .withRequestBody(EqualToJsonPattern(createArenaOppgaveRequestjson, true, true))
                .willReturn(
                    WireMock.serverError()
                )
        )

        OppslagHttpClient(wireMockRule.url("")).createOppgave(createArenaOppgaveRequest)
    }

    @Test
    fun `get saker`() {

        val getArenaSakerRequest = GetArenaSakerRequest("12345678912", "PERSON")

        val getArenaSakerRequestjson = """
        {
            "fødselsnummer": "12345678912",
            "brukerType": "PERSON",
            "tema": "DAG",
            "includeInactive": false
        }"""

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/arena/getsaker"))
                .withRequestBody(EqualToJsonPattern(getArenaSakerRequestjson, true, true))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "saker": [
                                    { "sakId": "1458", "sakstatus": "AKTIV", "opprettet":"2018-11-20T14:13:04+00:00"},
                                    { "sakId": "1467", "sakstatus": "AKTIV", "opprettet":"2018-11-21T14:13:04+00:00"}
                                ]
                            }
                        """.trimIndent()
                        )
                )
        )

        val saker = OppslagHttpClient(wireMockRule.url("")).getSaker(getArenaSakerRequest)
        assertEquals("1458", saker[0].sakId)
        assertEquals("1467", saker[1].sakId)
        assertEquals(2, saker.size)
    }


    @Test(expected = OppslagException::class)
    fun `get saker feiler`() {

        val getArenaSakerRequest = GetArenaSakerRequest("12345678912", "PERSON")

        val getArenaSakerRequestjson = """
        {
            "fødselsnummer": "12345678912",
            "brukerType": "PERSON",
            "tema": "DAG",
            "includeInactive": false
        }"""

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/arena/getsaker"))
                .withRequestBody(EqualToJsonPattern(getArenaSakerRequestjson, true, true))
                .willReturn(
                    WireMock.serverError()
                )
        )

        val saker = OppslagHttpClient(wireMockRule.url("")).getSaker(getArenaSakerRequest)
    }
}
package no.nav.dagpenger.journalføring.arena.adapter.soap

import no.nav.cxf.metrics.MetricFeature
import no.nav.dagpenger.journalføring.arena.SakVedtakService
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import no.nav.virksomhet.tjenester.sak.arbeidogaktivitet.v1.ArbeidOgAktivitet
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object SoapPort {

    fun sakVedtakService(serviceUrl: String): SakVedtakService {
        return createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = SakVedtakService::class.java,
            wsdl = "wsdl/hentsak/arenaSakVedtakService.wsdl",
            namespace = "http://arena.nav.no/services/sakvedtakservice",
            svcName = "ArenaSakVedtakService",
            portName = "ArenaSakVedtakServicePort"
        )
    }

    fun behandleArbeidOgAktivitetOppgaveV1(serviceUrl: String): BehandleArbeidOgAktivitetOppgaveV1 {

        return createServicePort(
            serviceUrl,
            serviceClazz = BehandleArbeidOgAktivitetOppgaveV1::class.java,
            wsdl = "wsdl/no/nav/tjeneste/virksomhet/behandleArbeidOgAktivitetOppgave/v1/Binding.wsdl",
            namespace = "http://nav.no/tjeneste/virksomhet/behandleArbeidOgAktivitetOppgave/v1/Binding",
            svcName = "BehandleArbeidOgAktivitetOppgave_v1",
            portName = "BehandleArbeidOgAktivitetOppgave_v1Port"
        )
    }

    fun arenaArbeidOgAktivitet(serviceUrl: String): ArbeidOgAktivitet {
        return createServicePort(
            serviceUrl,
            serviceClazz = ArbeidOgAktivitet::class.java,
            wsdl = "wsdl/nav-tjeneste-arbeidOgAktivitet_ArbeidOgAktivitetWSEXP.wsdl",
            namespace = "http://nav.no/virksomhet/tjenester/sak/arbeidogaktivitet/v1",
            svcName = "ArbeidOgAktivitetWSEXP_ArbeidOgAktivitetHttpService",
            portName = "ArbeidOgAktivitetWSEXP_ArbeidOgAktivitetHttpPort"
        )
    }

    private fun <PORT_TYPE> createServicePort(
        serviceUrl: String,
        serviceClazz: Class<PORT_TYPE>,
        wsdl: String,
        namespace: String,
        svcName: String,
        portName: String
    ): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = serviceUrl
            wsdlURL = wsdl
            serviceName = QName(namespace, svcName)
            endpointName = QName(namespace, portName)
            serviceClass = serviceClazz
            features = listOf(WSAddressingFeature(), LoggingFeature(), MetricFeature())
            outInterceptors.add(CallIdInterceptor())
        }

        return factory.create(serviceClazz)
    }
}

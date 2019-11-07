package no.nav.dagpenger.journalføring.arena.adapter.soap

import no.nav.arena.services.sakvedtakservice.SakVedtakPortType
import no.nav.cxf.metrics.MetricFeature
import no.nav.tjeneste.virksomhet.behandlearbeidogaktivitetoppgave.v1.BehandleArbeidOgAktivitetOppgaveV1
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object SoapPort {


    fun ArenaSakVedtakService(serviceUrl: String): SakVedtakPortType {
        return createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = SakVedtakPortType::class.java,
            wsdl = "wsdl/arenaSakVedtakService.wsdl",
            namespace = "http://arena.nav.no/services/sakvedtakservice",
            svcName = "ArenaSakVedtakService",
            portName = "ArenaSakVedtakServicePort"
        )

    }

    fun BehandleArbeidOgAktivitetOppgaveV1(serviceUrl: String): BehandleArbeidOgAktivitetOppgaveV1 {

        return createServicePort(
            serviceUrl,
            serviceClazz = BehandleArbeidOgAktivitetOppgaveV1::class.java,
            wsdl = "wsdl/no/nav/tjeneste/virksomhet/behandleArbeidOgAktivitetOppgave/v1/Binding.wsdl",
            namespace = "http://nav.no/tjeneste/virksomhet/behandleArbeidOgAktivitetOppgave/v1/Binding",
            svcName = "BehandleArbeidOgAktivitetOppgave_v1",
            portName = "BehandleArbeidOgAktivitetOppgave_v1Port"
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

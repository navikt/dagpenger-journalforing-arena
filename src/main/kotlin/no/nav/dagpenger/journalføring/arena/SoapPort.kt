package no.nav.dagpenger.journalføring.arena

import no.nav.cxf.metrics.MetricFeature
/*import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3*/
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object SoapPort {

    /*fun PersonV3(serviceUrl: String): PersonV3 {

        return createServicePort(
            serviceUrl,
            serviceClazz = PersonV3::class.java,
            wsdl = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl",
            namespace = "http://nav.no/tjeneste/virksomhet/person/v3/Binding",
            svcName = "Person_v3",
            portName = "Person_v3Port"
        )
    }*/

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

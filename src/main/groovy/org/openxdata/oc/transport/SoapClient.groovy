package org.openxdata.oc.transport

import java.util.Collection

import org.openxdata.oc.ODMBuilder
import org.openxdata.oc.exception.ImportODMException

class SoapClient {

	def url
	def header

	SoapClient(def url, def userName, def password){
		this.url = url
		buildHeader(userName, password)
	}

	private def buildHeader(def user, def password){
		header = """<soapenv:Header>
					  <wsse:Security soapenv:mustUnderstand="1" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
					        <wsse:UsernameToken wsu:Id="UsernameToken-27777511" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
					            <wsse:Username>""" + user + """</wsse:Username>
					            <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">"""+password+"""</wsse:Password>
					        </wsse:UsernameToken>
					  </wsse:Security></soapenv:Header>"""
	}

	private String buildEnvelope(String body) {
		def envelope = """<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:v1="http://openclinica.org/ws/study/v1" xmlns:bean="http://openclinica.org/ws/beans">""" + header + body +
				"""</soapenv:Envelope>"""
		return envelope
	}

	private Node sendRequest(String envelope) {
		def outs = envelope.getBytes()

		def host = new java.net.URL(url)
		HttpURLConnection conn = host.openConnection()

		conn.setRequestMethod("POST")
		conn.setDoOutput(true)

		conn.setRequestProperty("Content-Length", outs.length.toString())
		conn.setRequestProperty("Content-Type", "text/xml")

		def os = conn.getOutputStream()
		os.write(outs)
		def is = conn.getInputStream()
		def builder = new StringBuilder()
		for (String s :is.readLines()) {
			builder.append(s)
		}
		def xml = new XmlParser().parseText(builder.toString())
		return xml
	}

	private def getMetadata(def studyOID) {
		def body = """<soapenv:Body>
					      <v1:getMetadataRequest>
					         <v1:studyMetadata>
					            <bean:studyRef>
					               <bean:identifier>"""+ studyOID +"""</bean:identifier>
					               <!--Optional:-->
					               <bean:siteRef>
					                  <bean:identifier>?</bean:identifier>
					               </bean:siteRef>
					            </bean:studyRef>
					         </v1:studyMetadata>
					      </v1:getMetadataRequest>
					   </soapenv:Body>"""

		def envelope = buildEnvelope(body)
		def xml = sendRequest(envelope)
		return xml.depthFirst().odm
	}

	private def listAll(){
		def body = """<soapenv:Body><v1:listAllRequest>?</v1:listAllRequest></soapenv:Body>"""

		def envelope = buildEnvelope(body)
		def xml = sendRequest(envelope)
		return xml;
	}

	public def importData(Collection<String> instanceData){
		def importODM = new ODMBuilder().buildODM(instanceData)
		def body = """<soapenv:Body>
					  	<v1:importRequest>""" +importODM + """</v1:importRequest>
					  </soapenv:Body>"""

		def envelope = buildEnvelope(body)
		def reply = sendRequest(envelope)

		def result = reply.depthFirst().result[0].text()
		if(result != "Success")
			throw new ImportODMException(reply.depthFirst().error[0].text())

		return result;
	}
}

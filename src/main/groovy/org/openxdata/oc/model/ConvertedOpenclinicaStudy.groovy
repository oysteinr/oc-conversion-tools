package org.openxdata.oc.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString;


/**
 * Simple representation of the OpenClinica Study.
 */
@ToString(includeNames=true)
@EqualsAndHashCode public class ConvertedOpenclinicaStudy {
	
	String OID
	String identifier
	String name
}

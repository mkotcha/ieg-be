package org.emmek.IEG.helpers.xml;

import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.util.Map;

@XmlRootElement(name = "Ea")
//@ToString
public class MisuraOraria {
    @XmlAnyAttribute
    private Map<String, String> attributi;

    @XmlValue
    private String valore;

}

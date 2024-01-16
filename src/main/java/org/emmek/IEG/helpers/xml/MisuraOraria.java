package org.emmek.IEG.helpers.xml;

import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@XmlRootElement(name = "Ea")
@NoArgsConstructor
@Setter
//@ToString
public class MisuraOraria {
    @XmlAnyAttribute
    public Map<String, String> attributi;

    @XmlValue
    public String valore;

}

package org.emmek.IEG.helpers.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.ToString;

@XmlRootElement(name = "DatiPod")
@ToString
public class DatiPod {
    @XmlElement(name = "Pod")
    public String pod;
    @XmlElement(name = "MeseAnno")
    public String meseAnno;

    @XmlElement(name = "Misura")
    public Misura misura;


}
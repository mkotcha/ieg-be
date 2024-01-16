package org.emmek.IEG.helpers.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlRootElement(name = "DatiPod")
@NoArgsConstructor
@Setter
@ToString
public class DatiPod {
    @XmlElement(name = "Pod")
    public String pod;

    @XmlElement(name = "MeseAnno")
    public String meseAnno;

    @XmlElement(name = "DataMisura")
    public String dataMisura;

    @XmlElement(name = "DatiPdp")
    public DatiPdp datiPdp;

    @XmlElement(name = "Misura")
    public Misura misura;


}
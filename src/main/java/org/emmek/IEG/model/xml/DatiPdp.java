package org.emmek.IEG.model.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlRootElement(name = "DatiPdp")
@NoArgsConstructor
@Setter
@ToString
public class DatiPdp {

    @XmlElement(name = "Trattamento")
    public String trattamento;

    @XmlElement(name = "Tensione")
    public Double tensione;

    @XmlElement(name = "Forfait")
    public String forfait;

    @XmlElement(name = "GruppoMis")
    public String gruppoMis;

    @XmlElement(name = "Ka")
    public String ka;

    @XmlElement(name = "Kr")
    public String kr;

    @XmlElement(name = "Kp")
    public String kp;
}

package org.emmek.IEG.helpers.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DatiPdp")
public class DatiPdp {

    @XmlElement(name = "Trattamento")
    public String trattamento;

    @XmlElement(name = "Tensione")
    public double tensione;

    @XmlElement(name = "Forfait")
    public String forfait;

    @XmlElement(name = "GruppoMis")
    public String gruppoMis;

    @XmlElement(name = "Ka")
    double ka;

    @XmlElement(name = "Kr")
    double kr;

    @XmlElement(name = "Kp")
    double kp;
}

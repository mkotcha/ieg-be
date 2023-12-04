package org.emmek.IEG.helpers;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.ToString;

@XmlRootElement(name = "Misura")
@ToString
public class Misura {
    @XmlElement(name = "Raccolta")
    public String raccolta;
    @XmlElement(name = "TipoDato")
    public String tipoDato;
    @XmlElement(name = "CausaOstativa")
    private String causaOstativa;
    @XmlElement(name = "Validato")
    private String validato;
    @XmlElement(name = "PotMax")
    private String potMax;
    @XmlElement(name = "Ea")
    private MisuraOraria ea;
    @XmlElement(name = "Er")
    private MisuraOraria er;
    @XmlElement(name = "EaF1")
    private String eaF1;
    @XmlElement(name = "EaF2")
    private String eaF2;
    @XmlElement(name = "EaF3")
    private String eaF3;
    @XmlElement(name = "ErF1")
    private String erF1;
    @XmlElement(name = "ErF2")
    private String erF2;
    @XmlElement(name = "ErF3")
    private String erF3;
    @XmlElement(name = "PotF1")
    private String potF1;
    @XmlElement(name = "PotF2")
    private String potF2;
    @XmlElement(name = "PotF3")
    private String potF3;
}

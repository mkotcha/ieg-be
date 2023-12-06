package org.emmek.IEG.helpers.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@XmlRootElement(name = "Misura")
@NoArgsConstructor
@ToString
@Setter
public class Misura {
    @XmlElement(name = "Raccolta")
    public String raccolta;
    @XmlElement(name = "TipoDato")
    public String tipoDato;
    @XmlElement(name = "CausaOstativa")
    public String causaOstativa;
    @XmlElement(name = "Validato")
    public String validato;
    @XmlElement(name = "PotMax")
    public String potMax;
    @XmlElement(name = "Ea")
    public List<MisuraOraria> ea;
    @XmlElement(name = "Er")
    public List<MisuraOraria> er;
    @XmlElement(name = "EaF1")
    public String eaF1;
    @XmlElement(name = "EaF2")
    public String eaF2;
    @XmlElement(name = "EaF3")
    public String eaF3;
    @XmlElement(name = "ErF1")
    public String erF1;
    @XmlElement(name = "ErF2")
    public String erF2;
    @XmlElement(name = "ErF3")
    public String erF3;
    @XmlElement(name = "PotF1")
    public String potF1;
    @XmlElement(name = "PotF2")
    public String potF2;
    @XmlElement(name = "PotF3")
    public String potF3;
}

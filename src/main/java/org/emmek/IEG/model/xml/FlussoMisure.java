package org.emmek.IEG.model.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@XmlRootElement(name = "FlussoMisure")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@ToString
public class FlussoMisure {
    // es. PDO2G (periodico), SNM2G (switching), VNO (voltura), RSN2G (rettifica switching)
    @XmlAttribute(name = "CodFlusso")
    public String codFlusso;

    @XmlElement(name = "DatiPod")
    public List<DatiPod> datiPod;
}

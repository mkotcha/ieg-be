package org.emmek.IEG.helpers.xml;

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
    @XmlElement(name = "DatiPod")
    public List<DatiPod> datiPod;
}

package org.emmek.IEG.helpers;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.*;

@XmlRootElement(name = "FlussoMisure")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class FlussoMisure {
    @XmlElement(name = "DatiPod")
    public DatiPod datiPod;


}

package org.emmek.IEG.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CodiceDistributore {
    EDIST("E-Distribuzione S.p.a (ENELD)", "800.900.800"),
    A2A("A2A Reti Elettriche S.p.A. (AEMMILANO)", "800 933 301");
    final String label;
    final String telefono;
}

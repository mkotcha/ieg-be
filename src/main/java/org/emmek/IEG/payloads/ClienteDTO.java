package org.emmek.IEG.payloads;

public record ClienteDTO(
        String cap,
        String cf,
        String comune,
        String email,
        String indirizzo,
        String pIva,
        String provincia,
        String ragioneSociale,
        String telefono
) {
}

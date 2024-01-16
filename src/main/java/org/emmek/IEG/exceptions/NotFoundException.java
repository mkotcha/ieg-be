package org.emmek.IEG.exceptions;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    public NotFoundException(long id) {
        super("Elemento con id " + id + " non trovato!");
    }

    public NotFoundException(String message) {
        super(message);
    }
}

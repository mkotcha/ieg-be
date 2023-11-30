package org.emmek.IEG.exceptions;

import java.util.Date;

public record ErrorsResponseDTO(String message, Date timestamp) {
}

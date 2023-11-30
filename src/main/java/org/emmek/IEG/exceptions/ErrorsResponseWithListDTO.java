package org.emmek.IEG.exceptions;

import io.swagger.v3.oas.annotations.Hidden;

import java.util.Date;
import java.util.List;

@Hidden
public record ErrorsResponseWithListDTO(
        Date timestamp,
        List<String> errorsList) {
}

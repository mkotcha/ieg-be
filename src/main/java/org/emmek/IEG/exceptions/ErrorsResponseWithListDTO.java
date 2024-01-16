package org.emmek.IEG.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter

public class ErrorsResponseWithListDTO extends ErrorsResponseDTO {
    List<String> errorsList;

    public ErrorsResponseWithListDTO(String message, Date date, List<String> errorsList) {
        super(message, date);
        this.errorsList = errorsList;

    }
}

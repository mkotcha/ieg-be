package org.emmek.IEG.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Setter
@Getter
public class ErrorsResponseDTO {
    String message;
    Date timestamp;
}

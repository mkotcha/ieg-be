package org.emmek.IEG.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class ExceptionsHandler {
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorsResponseWithListDTO handleBadRequest(BadRequestException e) {
        if (e.getErrorList() != null) {
            List<String> errorsList = e.getErrorList().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
            return new ErrorsResponseWithListDTO(new Date(), errorsList);
        } else {
            return new ErrorsResponseWithListDTO(new Date(), new ArrayList<>());
        }
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorsResponseDTO handleUnauthorized(UnauthorizedException e) {
        return new ErrorsResponseDTO(e.getMessage(), new Date());
    }

//    @ExceptionHandler(AccessDeniedException.class)
//    @ResponseStatus(HttpStatus.FORBIDDEN)
//    public ErrorsResponseDTO handleAccessDenied(AccessDeniedException e) {
//        return new ErrorsResponseDTO(e.getMessage(), new Date());
//    }


//    @ExceptionHandler(NotFoundException.class)
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    public ErrorsResponseDTO handleNotFound(NotFoundException e) {
//        return new ErrorsResponseDTO(e.getMessage(), new Date());
//    }

//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public ErrorsResponseDTO handleJsonError() {
//        return new ErrorsResponseDTO("Errore nel formato json , assicurati che ci siano gli apici in ogni proprietà e che le virgole siano presenti.", new Date());
//    }

//    @ExceptionHandler(RuntimeException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public ErrorsResponseDTO handleJsonError(RuntimeException e) {
//        return new ErrorsResponseDTO(e.getMessage(), new Date());
//    }


//    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
//    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
//    public ErrorsResponseDTO handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
//        return new ErrorsResponseDTO(e.getMessage(), new Date());
//    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorsResponseDTO handleGeneric(Exception e) {
        log.error("Server Error", e);
        return new ErrorsResponseDTO("Server Error", new Date());
    }
}

package oleg.sopilnyak.test.endpoint.rest;

import lombok.*;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.CannotRegisterToCourseException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Exception handler adviser
 */
@RestControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = {RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorMessage unknownException(Throwable ex, WebRequest req) {
        return RestErrorMessage.builder()
                .errorCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorMessage(ex.getMessage())
                .errorUrl(req.getContextPath())
                .build();
    }


    @ExceptionHandler(value = {ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RestErrorMessage onException(ResourceNotFoundException ex, WebRequest req) {
        return RestErrorMessage.builder()
                .errorCode(HttpStatus.NOT_FOUND.value())
                .errorMessage(ex.getMessage())
                .errorUrl(req.getContextPath())
                .build();
    }

    @ExceptionHandler(value = {CannotDeleteResourceException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public RestErrorMessage onException(CannotDeleteResourceException ex, WebRequest req) {
        return RestErrorMessage.builder()
                .errorCode(HttpStatus.CONFLICT.value())
                .errorMessage(ex.getMessage())
                .errorUrl(req.getContextPath())
                .build();
    }

    @ExceptionHandler(value = {CannotRegisterToCourseException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public RestErrorMessage onException(CannotRegisterToCourseException ex, WebRequest req) {
        return RestErrorMessage.builder()
                .errorCode(HttpStatus.CONFLICT.value())
                .errorMessage(ex.getMessage())
                .errorUrl(req.getContextPath())
                .build();
    }

    // inner classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestErrorMessage {
        private int errorCode;
        private String errorMessage;
        private String errorUrl;
    }
}

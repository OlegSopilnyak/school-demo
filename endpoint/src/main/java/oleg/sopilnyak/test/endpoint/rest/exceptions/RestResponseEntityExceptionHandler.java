package oleg.sopilnyak.test.endpoint.rest.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.CannotDoRestCallException;
import oleg.sopilnyak.test.endpoint.exception.CannotRegisterToCourseException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Exception handler advise (wrap all annotated rest-controller methods)
 */
@Slf4j
@RestControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = {RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorMessage unknownException(Throwable ex, WebRequest req) {
        return errorMessageFor(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
    }


    @ExceptionHandler(value = {CannotDoRestCallException.class})
    public ResponseEntity<RestErrorMessage> onException(CannotDoRestCallException ex, WebRequest req) {
        log.error("Cannot do rest call", ex);
        final RestErrorMessage response;
        final Throwable cause = ex.getCause();
        if (cause instanceof CannotRegisterToCourseException cannotRegisterToCourseException) {
            response = onException(cannotRegisterToCourseException, req);
        } else if (cause instanceof ResourceNotFoundException resourceNotFoundException) {
            response = onException(resourceNotFoundException, req);
        } else if (cause instanceof CannotDeleteResourceException cannotDeleteResourceException) {
            response = onException(cannotDeleteResourceException, req);
        } else {
            response = unknownException(cause, req);
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.errorCode));
    }

    @ExceptionHandler(value = {ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RestErrorMessage onException(ResourceNotFoundException ex, WebRequest req) {
        return errorMessageFor(HttpStatus.NOT_FOUND, ex, req);
    }

    @ExceptionHandler(value = {CannotDeleteResourceException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public RestErrorMessage onException(CannotDeleteResourceException ex, WebRequest req) {
        return errorMessageFor(HttpStatus.CONFLICT, ex, req);
    }

    @ExceptionHandler(value = {CannotRegisterToCourseException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public RestErrorMessage onException(CannotRegisterToCourseException ex, WebRequest req) {
        return errorMessageFor(HttpStatus.CONFLICT, ex, req);
    }

    // private methods
    private static RestErrorMessage errorMessageFor(HttpStatus status, Throwable ex, WebRequest req) {
        return RestErrorMessage.builder()
                .errorCode(status.value())
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

package oleg.sopilnyak.test.endpoint.rest.exceptions;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.EntityUnableProcessException;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;
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

    @ExceptionHandler(value = {CannotProcessActionException.class})
    public ResponseEntity<ActionErrorMessage> onException(CannotProcessActionException ex, WebRequest req) {
        log.error("Cannot do facade's action", ex);
        final ActionErrorMessage response;
        final Throwable cause = ex.getCause();
        if (cause instanceof SchoolAccessDeniedException accessDeniedException) {
            response = onException(accessDeniedException, req);
        } else if (cause instanceof EntityNotFoundException notFoundException) {
            response = onException(notFoundException, req);
        } else if (cause instanceof EntityUnableProcessException unableProcessException) {
            response = onException(unableProcessException, req);
        } else {
            response = unknownException(cause, req);
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.errorCode));
    }

    @ExceptionHandler(value = {SchoolAccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ActionErrorMessage onException(SchoolAccessDeniedException ex, WebRequest req) {
        log.error("Access denied", ex);
        return errorMessageFor(HttpStatus.FORBIDDEN, ex, req);
    }

    @ExceptionHandler(value = {EntityNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ActionErrorMessage onException(EntityNotFoundException ex, WebRequest req) {
        log.error("Entity not found", ex);
        return errorMessageFor(HttpStatus.NOT_FOUND, ex, req);
    }

    @ExceptionHandler(value = {EntityUnableProcessException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ActionErrorMessage onException(EntityUnableProcessException ex, WebRequest req) {
        log.error("Entity unable processed", ex);
        return errorMessageFor(HttpStatus.CONFLICT, ex, req);
    }

    @ExceptionHandler(value = {GeneralCannotDeleteException.class})
    public ResponseEntity<ActionErrorMessage> onException(GeneralCannotDeleteException ex, WebRequest req) {
        log.error("Cannot delete entity", ex);
        final Throwable cause = ex.getCause();
        final ActionErrorMessage response;
        if (cause instanceof SchoolAccessDeniedException accessDeniedException) {
            response = onException(accessDeniedException, req);
        } else if (cause instanceof EntityNotFoundException notFoundException) {
            response = onException(notFoundException, req);
        } else if (cause instanceof EntityUnableProcessException unableProcessException) {
            response = onException(unableProcessException, req);
        } else {
            response = unknownException(cause, req);
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.errorCode));
    }

    @ExceptionHandler(value = {RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ActionErrorMessage unknownException(Throwable ex, WebRequest req) {
        return errorMessageFor(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
    }

    // private methods
    private static ActionErrorMessage errorMessageFor(HttpStatus status, Throwable ex, WebRequest req) {
        return ActionErrorMessage.builder()
                .errorCode(status.value()).errorMessage(ex.getMessage()).errorUrl(req.getDescription(false))
                .build();
    }

}

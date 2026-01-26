package oleg.sopilnyak.test.endpoint.rest.exceptions;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.UnableProcessEntityException;
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
 *
 * @see ActionErrorMessage
 * @see ResponseEntity
 */
@Slf4j
@RestControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = {CannotProcessActionException.class})
    public ResponseEntity<ActionErrorMessage> onException(CannotProcessActionException exception, WebRequest request) {
        log.error("Cannot do facade's action", exception);
        final var response = prepareResponseFor(exception, request);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.errorCode));
    }

    @ExceptionHandler(value = {SchoolAccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ActionErrorMessage onException(SchoolAccessDeniedException exception, WebRequest req) {
        log.error("Access denied", exception);
        return errorMessageFor(HttpStatus.FORBIDDEN, exception, req);
    }

    @ExceptionHandler(value = {EntityNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ActionErrorMessage onException(EntityNotFoundException exception, WebRequest req) {
        log.error("Entity not found", exception);
        return errorMessageFor(HttpStatus.NOT_FOUND, exception, req);
    }

    @ExceptionHandler(value = {UnableProcessEntityException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ActionErrorMessage onException(UnableProcessEntityException exception, WebRequest req) {
        log.error("Entity unable processed", exception);
        return errorMessageFor(HttpStatus.CONFLICT, exception, req);
    }

    @ExceptionHandler(value = {GeneralCannotDeleteException.class})
    public ResponseEntity<ActionErrorMessage> onException(GeneralCannotDeleteException exception, WebRequest request) {
        log.error("Cannot delete entity", exception);
        final var response = prepareResponseFor(exception, request);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.errorCode));
    }

    @ExceptionHandler(value = {RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ActionErrorMessage unknownException(Throwable exception, WebRequest req) {
        return errorMessageFor(HttpStatus.INTERNAL_SERVER_ERROR, exception, req);
    }

    // private methods
    private ActionErrorMessage prepareResponseFor(final RuntimeException exception, final WebRequest request) {
        return switch (exception.getCause()) {
            case SchoolAccessDeniedException noAccessException -> onException(noAccessException, request);
            case EntityNotFoundException notFoundException -> onException(notFoundException, request);
            case UnableProcessEntityException processException -> onException(processException, request);
            case null, default -> unknownException(exception.getCause(), request);
        };
    }

    private static ActionErrorMessage errorMessageFor(HttpStatus status, Throwable ex, WebRequest req) {
        return ActionErrorMessage.builder()
                .errorCode(status.value()).errorMessage(ex.getMessage()).errorUrl(req.getDescription(false))
                .build();
    }

}

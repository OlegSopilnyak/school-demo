package oleg.sopilnyak.test.school.common.exception.core;

import oleg.sopilnyak.test.school.common.exception.UnableProcessEntityException;

/**
 * Exception: throws when system cannot delete entity
 */
public class GeneralCannotDeleteException extends UnableProcessEntityException {
    public GeneralCannotDeleteException(String message) {
        super(message);
    }

    public GeneralCannotDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}

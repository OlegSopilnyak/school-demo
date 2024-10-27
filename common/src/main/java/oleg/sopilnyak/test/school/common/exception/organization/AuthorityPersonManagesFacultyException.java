package oleg.sopilnyak.test.school.common.exception.organization;

import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;

/**
 * Exception: throws when you want to delete authority person who is the dean of a faculty now
 */
public class AuthorityPersonManagesFacultyException extends GeneralCannotDeleteException {
    public AuthorityPersonManagesFacultyException(String message) {
        super(message);
    }

    public AuthorityPersonManagesFacultyException(String message, Throwable cause) {
        super(message, cause);
    }
}

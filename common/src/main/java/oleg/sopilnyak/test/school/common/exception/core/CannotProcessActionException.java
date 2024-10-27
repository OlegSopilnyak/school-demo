package oleg.sopilnyak.test.school.common.exception.core;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;

/**
 * Exception: throws when system cannot process facade's action
 */
public class CannotProcessActionException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "Cannot process the action '%2$s' for the facade '%1$s'.\n--\tBecause '%3$s'.";

    private CannotProcessActionException(String facadeName, String actionName) {
        super(makeMessageFor(facadeName, actionName));
    }

    private CannotProcessActionException(String facadeName, String actionName, String message) {
        super(makeMessageFor(facadeName, actionName, message));
    }

    public CannotProcessActionException(ActionContext context) {
        this(context.getFacadeName(), context.getActionName());
    }

    public CannotProcessActionException(ActionContext context, String message) {
        this(context.getFacadeName(), context.getActionName(), message);
    }

    public CannotProcessActionException(ActionContext context, String message, Throwable cause) {
        this(context.getFacadeName(), context.getActionName(), message, cause);
    }

    public CannotProcessActionException(String message) {
        this(ActionContext.current(), message);
    }

    public CannotProcessActionException(Throwable cause) {
        this(ActionContext.current(), cause.getMessage(), cause);
    }

    public CannotProcessActionException(String message, Throwable cause) {
        this(ActionContext.current(), message, cause);
    }

    private CannotProcessActionException(ActionContext context, Throwable cause) {
        this(context.getFacadeName(), context.getActionName(), cause);
    }

    private CannotProcessActionException(String facadeName, String actionName, Throwable cause) {
        super(makeMessageFor(facadeName, actionName), cause);
    }

    private CannotProcessActionException(String facadeName, String actionName, String message, Throwable cause) {
        super(makeMessageFor(facadeName, actionName, message), cause);
    }

    private static String makeMessageFor(String facadeName, String actionName) {
        return makeMessageFor(facadeName, actionName, "action failed");
    }

    private static String makeMessageFor(String facadeName, String actionName, String reason) {
        assert facadeName != null && !facadeName.isBlank() : "facadeName is invalid";
        assert actionName != null && !actionName.isBlank() : "actionName is invalid";
        return String.format(MESSAGE_TEMPLATE, facadeName, actionName, reason);
    }
}

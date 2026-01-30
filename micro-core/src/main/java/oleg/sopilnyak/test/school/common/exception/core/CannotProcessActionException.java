package oleg.sopilnyak.test.school.common.exception.core;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;

/**
 * Exception: throws when system cannot process facade's action
 */
public class CannotProcessActionException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE_PREFIX = "Cannot process the action '%2$s' for the facade '%1$s'.";
    private static final String MESSAGE_TEMPLATE = MESSAGE_TEMPLATE_PREFIX + "\n--\tBecause '%3$s'.";

    private CannotProcessActionException(String facadeName, String actionName) {
        super(makeMessageFor(facadeName, actionName));
    }

    private CannotProcessActionException(String facadeName, String actionName, String message) {
        super(makeMessageFor(facadeName, actionName, message));
    }

    public CannotProcessActionException(ActionContext context) {
        this(context.getActionProcessorFacade(), context.getEntryPointMethod());
    }

    public CannotProcessActionException(ActionContext context, String message) {
        this(context.getActionProcessorFacade(), context.getEntryPointMethod(), message);
    }

    public CannotProcessActionException(ActionContext context, String message, Throwable cause) {
        this(context.getActionProcessorFacade(), context.getEntryPointMethod(), message, cause);
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

    private CannotProcessActionException(String facadeName, String actionName, String message, Throwable cause) {
        super(makeMessageFor(facadeName, actionName, message), cause);
    }

    private static String makeMessageFor(String facadeName, String actionName) {
        return makeMessageFor(facadeName, actionName, "action failed");
    }

    private static String makeMessageFor(String facadeName, String actionName, String reason) {
        assert facadeName != null && !facadeName.isBlank() : "facadeName is invalid";
        assert actionName != null && !actionName.isBlank() : "actionName is invalid";
        return reason != null && reason.startsWith(String.format(MESSAGE_TEMPLATE_PREFIX, facadeName, actionName)) ?
                // just created the instance from deserializer
                reason :
                // the instance created from particular rest controller for throw
                String.format(MESSAGE_TEMPLATE, facadeName, actionName, reason);
    }
}

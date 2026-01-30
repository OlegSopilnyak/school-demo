package oleg.sopilnyak.test.school.common.business.facade;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * Type : context of facade action
 *
 * @see BusinessFacade#getName()
 */
@Data
@Builder
public class ActionContext implements Serializable {
    private static final ThreadLocal<ActionContext> CONTEXT = new ThreadLocal<>();
    // the name of method of the entry-point container (rest-controller, message-listener, etc.)
    private String entryPointMethod;
    // the name of facade which processed the action
    private String actionProcessorFacade;
    // the id of the action
    private String actionId;
    // the time when action processing is started
    @Builder.Default
    private Instant startedAt = Instant.now();
    // the duration of the action (how long it was proceeded)
    @Builder.Default
    private Duration lasts = Duration.ZERO;

    /**
     * To install new value of action context for current thread (replace not allowed)
     *
     * @param context instance to install
     */
    public static void install(ActionContext context) {
        install(context, false);
    }

    /**
     * To install new value of action context for current thread
     *
     * @param context instance to install
     * @param replace is context replace allowed
     */
    public static void install(ActionContext context, boolean replace) {
        if (CONTEXT.get() == null || replace) {
            CONTEXT.set(context);
        } else {
            throw new AssertionError("context is already installed");
        }
    }

    /**
     * To finish context's work
     */
    public void finish() {
        setLasts(Duration.between(startedAt, Instant.now()));
    }

    /**
     * To get current (for current thread) action context
     *
     * @return context instance
     */
    public static ActionContext current() {
        return CONTEXT.get();
    }

    /**
     * To release current context
     */
    public static void release() {
        CONTEXT.remove();
    }

    /**
     * To set up new instance of current context
     *
     * @param facade the name of context's facade
     * @param action the name of context's action
     */
    public static ActionContext setup(final String facade, final String action) {
        if (facade == null || facade.isBlank()) throw new AssertionError("facade name is empty");
        if (action == null || action.isBlank()) throw new AssertionError("action name is empty");
        CONTEXT.set(ActionContext.builder().actionProcessorFacade(facade.trim()).entryPointMethod(action.trim()).build());
        return CONTEXT.get();
    }
}

package oleg.sopilnyak.test.school.common.business.facade;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * Type : context of facade action
 */
@Data
@Builder
public class ActionContext implements Serializable {
    private static final ThreadLocal<ActionContext> CONTEXT = new ThreadLocal<>();
    private String facadeName;
    private String actionName;
    private String actionId;
    @Builder.Default
    private Instant startedAt = Instant.now();
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
        CONTEXT.set(ActionContext.builder().facadeName(facade.trim()).actionName(action.trim()).build());
        return CONTEXT.get();
    }
}

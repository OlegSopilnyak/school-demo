package oleg.sopilnyak.test.school.common.business.facade;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Type : context of facade action
 */
@Data
@Builder
public class ActionContext implements Serializable {
    private static final ThreadLocal<ActionContext> CONTEXT = new ThreadLocal<>();

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
     * @param facadeName the name of context's facade
     * @param actionName the name of context's action
     */
    public static void setup(final String facadeName, final String actionName) {
        assert facadeName != null && !facadeName.isBlank() : "facade name is empty";
        assert actionName != null && !actionName.isBlank() : "action name is empty";
        CONTEXT.set(new ActionContext(facadeName.trim(), actionName.trim()));
    }

    private String facadeName;
    private String actionName;
}

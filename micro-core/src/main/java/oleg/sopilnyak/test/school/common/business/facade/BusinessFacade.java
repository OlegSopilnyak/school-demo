package oleg.sopilnyak.test.school.common.business.facade;

/**
 * Service-Facade: The parent of any Service Facades (has name of the facade)
 */
public interface BusinessFacade {

    /**
     * To do action and return the result
     *
     * @param actionId the id of the action
     * @param actionParameters the parameters of action to execute
     * @param <T> type of action execution result
     * @return action execution result value
     */
    default <T> T doActionAndResult(String actionId, Object... actionParameters) {
        throw new UnsupportedOperationException("Please implement method in BusinessFacade's descendant.");
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     * @see ActionContext#getFacadeName()
     */
    String getName();
}

package oleg.sopilnyak.test.service.facade.education.base.impl;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.education.base.EducationFacade;

public abstract class EducationFacadeImpl implements EducationFacade {
    /**
     * Unified facade's entry-point to do action and return the result
     * delegate to facade's main entry-point method
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @return action execution result value
     * @see EducationFacade#educationAction(String, Object...)
     */
    @Override
    public <T> T doActionAndResult(final String actionId, Object... parameters) {
        return educationAction(actionId, parameters);
    }

    /**
     * Facade depends on the action's execution (organization action)
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @return action execution result value
     * @see BusinessFacade#doActionAndResult(String, Object...)
     */
    @Override
    public <T> T educationAction(String actionId, Object... parameters) {
        return EducationFacade.super.educationAction(actionId, parameters);
    }
}

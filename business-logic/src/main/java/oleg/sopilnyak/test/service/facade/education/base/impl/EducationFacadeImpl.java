package oleg.sopilnyak.test.service.facade.education.base.impl;

import oleg.sopilnyak.test.school.common.business.facade.education.base.EducationFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.base.EducationCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;

import org.slf4j.Logger;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class EducationFacadeImpl<T extends EducationCommand<?>> implements EducationFacade, ActionFacade {
    protected final CommandsFactory<T> factory;
    @Getter
    private final CommandActionExecutor actionExecutor;

    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        throw new UnsupportedOperationException("Please override this method.");
    }
}

package oleg.sopilnyak.test.service.facade.profile.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.profile.StudentProfileFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;

/**
 * Service: To process commands for school's student profiles facade
 */
@Slf4j
public class StudentProfileFacadeImpl<T>
        extends PersonProfileFacadeImpl<T>
        implements StudentProfileFacade {

    public StudentProfileFacadeImpl(CommandsFactory<T> factory) {
        super(factory);
    }

    @Override
    protected String findByIdCommandId() {
        return StudentProfileCommand.FIND_BY_ID_COMMAND_ID;
    }

    @Override
    protected String createOrUpdateCommandId() {
        return StudentProfileCommand.CREATE_OR_UPDATE_COMMAND_ID;
    }

    @Override
    protected String deleteByIdCommandId() {
        return StudentProfileCommand.DELETE_BY_ID_COMMAND_ID;
    }
}

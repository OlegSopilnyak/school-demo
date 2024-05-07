package oleg.sopilnyak.test.service.facade.profile.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.profile.StudentProfileFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;

import static oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand.*;

/**
 * Service: To process commands for school's student profiles facade
 */
@Slf4j
public class StudentProfileFacadeImpl
        extends PersonProfileFacadeImpl<StudentProfileCommand>
        implements StudentProfileFacade {

    public StudentProfileFacadeImpl(CommandsFactory<StudentProfileCommand> factory) {
        super(factory);
    }

    @Override
    protected final String findByIdCommandId() {
        return FIND_BY_ID_COMMAND_ID;
    }

    @Override
    protected final String createOrUpdateCommandId() {
        return CREATE_OR_UPDATE_COMMAND_ID;
    }

    @Override
    protected final String deleteByIdCommandId() {
        return DELETE_BY_ID_COMMAND_ID;
    }
}

package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to get authority person by id
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindAuthorityPersonCommand implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    private final AuthorityPersonPersistenceFacade persistenceFacade;

    /**
     * To find authority person by id
     *
     * @param parameter system authority-person-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<AuthorityPerson>> execute(Object parameter) {
        try {
            log.debug("Trying to find authority person by ID:{}", parameter);
            Long id = commandParameter(parameter);
            Optional<AuthorityPerson> person = persistenceFacade.findAuthorityPersonById(id);
            log.debug("Got authority person {} by ID:{}", person, id);
            return CommandResult.<Optional<AuthorityPerson>>builder()
                    .result(Optional.ofNullable(person))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the authority person by ID:{}", parameter, e);
            return CommandResult.<Optional<AuthorityPerson>>builder()
                    .result(Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To find course by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see CoursesPersistenceFacade#findCourseById(Long)
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find course by ID:{}", parameter.toString());

            final Long id = commandParameter(parameter);
            Optional<AuthorityPerson> person = persistenceFacade.findAuthorityPersonById(id);

            log.debug("Got authority person {} by ID:{}", person, id);
            context.setResult(person);
        } catch (Exception e) {
            log.error("Cannot find the authority person by ID:{}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return FIND_BY_ID;
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}

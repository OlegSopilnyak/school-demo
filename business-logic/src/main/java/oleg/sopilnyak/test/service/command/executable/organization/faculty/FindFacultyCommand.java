package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to get faculty by id
 *
 * @see Faculty
 * @see FacultyCommand
 * @see FacultyPersistenceFacade
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindFacultyCommand implements FacultyCommand {
    private final FacultyPersistenceFacade persistenceFacade;

    /**
     * DO: To find faculty by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see FacultyPersistenceFacade#findFacultyById(Long)
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find faculty by ID:{}", parameter);
            final Long id = commandParameter(parameter);

            final Optional<Faculty> entity = persistenceFacade.findFacultyById(id);

            log.debug("Got faculty {} by ID:{}", entity, id);
            context.setResult(entity);
        } catch (Exception e) {
            log.error("Cannot find the faculty by ID:{}", parameter, e);
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

package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to get faculty by id
 *
 * @see Faculty
 * @see FacultyCommand
 * @see FacultyPersistenceFacade
 */
@Slf4j
@AllArgsConstructor
@Getter
@Component("facultyFind")
public class FindFacultyCommand implements FacultyCommand<Optional<Faculty>> {
    private final transient FacultyPersistenceFacade persistenceFacade;
    private final transient BusinessMessagePayloadMapper payloadMapper;

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
    public  void executeDo(Context<Optional<Faculty>> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final Long id = parameter.value();
            log.debug("Trying to find faculty by ID:{}", id);

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

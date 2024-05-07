package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to get authority person by id
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see AuthorityPersonPersistenceFacade
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindAuthorityPersonCommand implements AuthorityPersonCommand {
    private final AuthorityPersonPersistenceFacade persistenceFacade;

    /**
     * DO: To find authority person by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find authority person by ID:{}", parameter);
            final Long id = commandParameter(parameter);

            final Optional<AuthorityPerson> person = persistenceFacade.findAuthorityPersonById(id);

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

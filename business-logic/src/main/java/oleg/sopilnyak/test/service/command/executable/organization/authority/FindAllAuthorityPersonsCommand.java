package oleg.sopilnyak.test.service.command.executable.organization.authority;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to get all authority persons of the school
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see AuthorityPersonPersistenceFacade
 */
@Slf4j
@AllArgsConstructor
@Component("authorityPersonFindAll")
public class FindAllAuthorityPersonsCommand implements AuthorityPersonCommand<Set<AuthorityPerson>> {
    private final transient AuthorityPersonPersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * DO: To get all authority persons of the school<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     * @see AuthorityPersonPersistenceFacade#findAllAuthorityPersons()
     */
    @Override
    public void executeDo(Context<Set<AuthorityPerson>> context) {
        try {
            // no command do input
            log.debug("Trying to get all authority persons");

            final Set<AuthorityPerson> staff = persistence.findAllAuthorityPersons();

            log.debug("Got authority persons {}", staff);
            context.setResult(staff);
        } catch (Exception e) {
            log.error("Cannot find any authority person", e);
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
        return FIND_ALL;
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

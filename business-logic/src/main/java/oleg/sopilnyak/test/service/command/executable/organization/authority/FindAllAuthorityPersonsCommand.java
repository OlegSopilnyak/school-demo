package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to get all authority persons of the school
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see AuthorityPersonPersistenceFacade
 * @see BasicCommand#self()
 */
@Slf4j
@AllArgsConstructor
@Component(AuthorityPersonCommand.Component.FIND_ALL)
public class FindAllAuthorityPersonsCommand extends BasicCommand<Set<AuthorityPerson>>
        implements AuthorityPersonCommand<Set<AuthorityPerson>> {
    private final transient AuthorityPersonPersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.FIND_ALL;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return AuthorityPersonFacade.FIND_ALL;
    }

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
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void executeDo(Context<Set<AuthorityPerson>> context) {
        try {
            // no command do input
            log.debug("Trying to get all authority persons");

            final Set<AuthorityPerson> staff = persistence.findAllAuthorityPersons().stream()
                    .map(this::adoptEntity).collect(Collectors.toSet());

            log.debug("Got authority persons {}", staff);
            context.setResult(staff);
        } catch (Exception e) {
            log.error("Cannot find any authority person", e);
            context.failed(e);
        }
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

package oleg.sopilnyak.test.service.command.executable.organization.group;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all students groups of the school
 *
 * @see StudentsGroup
 * @see StudentsGroupCommand
 * @see StudentsGroupPersistenceFacade
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindAllStudentsGroupsCommand implements StudentsGroupCommand<Set<StudentsGroup>> {
    private final StudentsGroupPersistenceFacade persistence;

    /**
     * To get all students groups of the school
     *
     * @param parameter not used
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Set<StudentsGroup>> execute(Object parameter) {
        try {
            log.debug("Trying to get all students groups");
            final Set<StudentsGroup> groups = persistence.findAllStudentsGroups();
            log.debug("Got students groups {}", groups);
            return CommandResult.<Set<StudentsGroup>>builder()
                    .result(Optional.ofNullable(groups))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find any students group", e);
            return CommandResult.<Set<StudentsGroup>>builder()
                    .result(Optional.of(Set.of())).exception(e).success(false).build();
        }
    }

    /**
     * DO: To get all students groups of the school<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     * @see StudentsGroupPersistenceFacade#findAllStudentsGroups()
     */
    @Override
    public void executeDo(Context<?> context) {
        try {
            log.debug("Trying to get all students groups");

            final Set<StudentsGroup> groups = persistence.findAllStudentsGroups();

            log.debug("Got students groups {}", groups);
            context.setResult(groups);
        } catch (Exception e) {
            log.error("Cannot find any students groups", e);
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

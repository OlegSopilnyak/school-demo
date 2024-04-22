package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to delete the authority person by id
 */
@Slf4j
@AllArgsConstructor
@Component
public class DeleteAuthorityPersonCommand implements AuthorityPersonCommand<Boolean> {
    private final AuthorityPersonPersistenceFacade persistenceFacade;

    /**
     * To delete authority person by id
     *
     * @param parameter system authority-person-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete authority person with ID: {}", parameter);
            Long id = commandParameter(parameter);
            Optional<AuthorityPerson> person = persistenceFacade.findAuthorityPersonById(id);
            if (person.isEmpty()) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new NotExistAuthorityPersonException("AuthorityPerson with ID:" + id + " is not exists."))
                        .success(false).build();
            } else if (!person.get().getFaculties().isEmpty()) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new AuthorityPersonManageFacultyException("AuthorityPerson with ID:" + id + " is managing faculties."))
                        .success(false).build();
            }

            persistenceFacade.deleteAuthorityPerson(id);

            log.debug("Deleted authority person {} {}", person.get(), true);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(true))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot delete the authority person by ID:{}", parameter, e);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(false))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return DELETE;
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

    /**
     * To delete authority person by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
//    @Override
//    public void executeDo(Context<?> context) {
//        final Object parameter = context.getRedoParameter();
//        try {
//            log.debug("Trying to delete authority person with ID: {}", parameter);
//            Long id = commandParameter(parameter);
//            Optional<AuthorityPerson> person = persistenceFacade.findAuthorityPersonById(id);
//            if (person.isEmpty()) {
//                return CommandResult.<Boolean>builder()
//                        .result(Optional.of(false))
//                        .exception(new NotExistAuthorityPersonException("AuthorityPerson with ID:" + id + " is not exists."))
//                        .success(false).build();
//            } else if (!person.get().getFaculties().isEmpty()) {
//                return CommandResult.<Boolean>builder()
//                        .result(Optional.of(false))
//                        .exception(new AuthorityPersonManageFacultyException("AuthorityPerson with ID:" + id + " is managing faculties."))
//                        .success(false).build();
//            }
//
//            persistenceFacade.deleteAuthorityPerson(id);
//
//            log.debug("Deleted authority person {} {}", person.get(), true);
//            return CommandResult.<Boolean>builder()
//                    .result(Optional.of(true))
//                    .success(true)
//                    .build();
//        } catch (Exception e) {
//            log.error("Cannot delete the authority person by ID:{}", parameter, e);
//            return CommandResult.<Boolean>builder()
//                    .result(Optional.of(false))
//                    .exception(e).success(false).build();
//        }
//    }

    /**
     * To delete authority person by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
//    @Override
//    public void executeUndo(Context<?> context) {
//        AuthorityPersonCommand.super.executeUndo(context);
//    }
}

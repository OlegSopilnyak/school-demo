package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.composite.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.composite.TransferResultVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;
import org.springframework.lang.NonNull;

/**
 * Type for school-organization authority persons management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public interface AuthorityPersonCommand extends OrganizationCommand {
    String PERSON_WITH_ID_PREFIX = "AuthorityPerson with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "authorityCommandsFactory";
    /**
     * Command-ID: for find all authority persons
     */
    String FIND_ALL = "organization.authority.person.findAll";
    /**
     * Command-ID: for find by ID authority person
     */
    String FIND_BY_ID = "organization.authority.person.findById";
    /**
     * Command-ID: for create or update authority person entity
     */
    String CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    /**
     * Command-ID: for delete authority person entity
     */
    String DELETE = "organization.authority.person.delete";

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(AuthorityPersonCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
        return visitor.prepareContext(this, input);
    }

    /**
     * To transfer command execution result to next command context
     *
     * @param visitor visitor for transfer result
     * @param result  result of command execution
     * @param target  command context for next execution
     * @param <S>     type of current command execution result
     * @param <T>     type of next command execution result
     * @see TransferResultVisitor#transferPreviousExecuteDoResult(AuthorityPersonCommand, Object, Context)
     * @see Context#setRedoParameter(Object)
     */
    @Override
    default <S, T> void transferResultTo(@NonNull final TransferResultVisitor visitor,
                                         final S result, final Context<T> target) {
        visitor.transferPreviousExecuteDoResult(this, result, target);
    }
}

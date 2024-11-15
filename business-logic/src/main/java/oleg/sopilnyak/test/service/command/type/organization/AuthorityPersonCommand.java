package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

/**
 * Type for school-organization authority persons management command
 *
 * @param <T> the type of command execution (do) result
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public interface AuthorityPersonCommand<T> extends OrganizationCommand<T> {
    String PERSON_WITH_ID_PREFIX = "AuthorityPerson with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "authorityCommandsFactory";
    /**
     * Command-ID: for log in person by login/password
     */
    String LOGIN = "organization.authority.person.login";
    /**
     * Command-ID: for log out person by auth token
     */
    String LOGOUT = "organization.authority.person.logout";
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
     * Command-ID: for create authority person entity with related profile
     */
    String CREATE_NEW = "organization.authority.person.create.macro";
    /**
     * Command-ID: for delete authority person entity
     */
    String DELETE = "organization.authority.person.delete";
    /**
     * Command-ID: for delete authority person entity with assigned profile
     */
    String DELETE_ALL = "organization.authority.person.delete.macro";

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input parameter
     *                            //     * @param <T>                   type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(AuthorityPersonCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Object macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
//     * @param <T>           type of command execution result
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see AuthorityPersonCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
//    @Override
////    default <T> void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
//    default void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
//                                   final Context<?> context,
//                                   final Context.StateChangedListener<?> stateListener) {
////        RootCommand<T>
//        visitor.doNestedCommand(this,
//                context,
//                stateListener);
//    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
//     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see AuthorityPersonCommand#undoCommand(Context)
     */
//    @Override
////    default <T> Context<T> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor,
//    default Context<T> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor,
//                                               final Context<T> context) {
//        return visitor.undoNestedCommand(this, context);
//    }
}

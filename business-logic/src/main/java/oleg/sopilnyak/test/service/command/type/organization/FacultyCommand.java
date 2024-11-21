package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

/**
 * Type for school-organization faculties management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public interface FacultyCommand<T> extends OrganizationCommand<T> {
    String FACULTY_WITH_ID_PREFIX = "Faculty with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "facultyCommandsFactory";
    /**
     * Command-ID: for find all faculties
     */
    String FIND_ALL = "organization.faculty.findAll";
    /**
     * Command-ID: for find by ID faculty
     */
    String FIND_BY_ID = "organization.faculty.findById";
    /**
     * Command-ID: for create or update faculty entity
     */
    String CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    /**
     * Command-ID: for delete faculty entity
     */
    String DELETE = "organization.faculty.delete";

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(FacultyCommand, Object)
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
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see FacultyCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    default void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                   final Context<?> context, final Context.StateChangedListener stateListener) {
        visitor.doNestedCommand(this, (Context<T>) context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see FacultyCommand#undoCommand(Context)
     */
    @Override
    default Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
        return visitor.undoNestedCommand(this, context);
    }
}

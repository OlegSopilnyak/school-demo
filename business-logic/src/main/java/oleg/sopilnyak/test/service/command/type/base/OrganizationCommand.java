package oleg.sopilnyak.test.service.command.type.base;

import org.slf4j.Logger;

/**
 * Type for organization entities management command
 *
 * @see oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand
 * @see oleg.sopilnyak.test.service.command.type.FacultyCommand
 * @see oleg.sopilnyak.test.service.command.type.StudentsGroupCommand
 */
public interface OrganizationCommand<T> extends SchoolCommand<T> {
    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To execute command
     *
     * @param context context of redo execution
     * @see Context
     */
    @Override
    default void doCommand(Context<?> context) {
        if (isWrongRedoStateOf(context)) {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            executeDo(context);
        }
    }

    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    default void undoCommand(Context<?> context) {
        if (isWrongUndoStateOf(context)) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            executeUndo(context);
        }
    }
}

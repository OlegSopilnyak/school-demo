package oleg.sopilnyak.test.service.command.type.organization.base;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import org.slf4j.Logger;

/**
 * Type for organization entities management command
 *
 * @see AuthorityPersonCommand
 * @see FacultyCommand
 * @see StudentsGroupCommand
 */
public interface OrganizationCommand extends RootCommand {
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
    default <T> void doCommand(Context<T> context) {
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
    default <T> void undoCommand(Context<T> context) {
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

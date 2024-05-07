package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import org.slf4j.Logger;

/**
 * Type for school-student command
 *
 * @see SchoolCommand
 * @see oleg.sopilnyak.test.school.common.model.Student
 */
public interface StudentCommand extends SchoolCommand {
    String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "studentCommandsFactory";

    /**
     * ID of student's findById command
     */
    String FIND_BY_ID_COMMAND_ID = "student.findById";
    /**
     * ID of student's findEnrolledTo command
     */
    String FIND_ENROLLED_COMMAND_ID = "student.findEnrolledTo";
    /**
     * ID of student's findNotEnrolled command
     */
    String FIND_NOT_ENROLLED_COMMAND_ID = "student.findNotEnrolled";
    /**
     * ID of student's createOrUpdate command
     */
    String CREATE_OR_UPDATE_COMMAND_ID = "student.createOrUpdate";
    /**
     * ID of student's delete command
     */
    String DELETE_COMMAND_ID = "student.delete";

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

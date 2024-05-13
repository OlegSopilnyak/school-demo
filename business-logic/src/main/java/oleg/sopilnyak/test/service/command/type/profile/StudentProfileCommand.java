package oleg.sopilnyak.test.service.command.type.profile;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.composite.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

/**
 * Type for school-student-profile commands
 */
public interface StudentProfileCommand extends ProfileCommand {
    /**
     * ID of findById student profile command
     */
    String FIND_BY_ID_COMMAND_ID = "profile.student.findById";
    /**
     * ID of deleteById student profile command
     */
    String DELETE_BY_ID_COMMAND_ID = "profile.student.deleteById";
    /**
     * ID of createOrUpdate student profile command
     */
    String CREATE_OR_UPDATE_COMMAND_ID = "profile.student.createOrUpdate";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "studentProfileCommandsFactory";

    /**
     * To prepare command context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     */
    @Override
    default <T> Context<T> acceptPreparedContext(PrepareContextVisitor visitor, Object input) {
        return visitor.prepareContext(this, input);
    }
}

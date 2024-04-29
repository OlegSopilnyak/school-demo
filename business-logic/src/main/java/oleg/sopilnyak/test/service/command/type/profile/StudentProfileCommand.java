package oleg.sopilnyak.test.service.command.type.profile;

import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

/**
 * Type for school-student-profile commands
 */
public interface StudentProfileCommand<T> extends ProfileCommand<T> {
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
}

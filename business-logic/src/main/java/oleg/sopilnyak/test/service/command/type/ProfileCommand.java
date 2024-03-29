package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

/**
 * Type for school-profile command
 */
public interface ProfileCommand<T> extends SchoolCommand<T> {
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "profileCommandsFactory";
}

package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

/**
 * Type for organization command
 */
public interface OrganizationCommand<T> extends SchoolCommand<T> {
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "organizationCommandsFactory";
}

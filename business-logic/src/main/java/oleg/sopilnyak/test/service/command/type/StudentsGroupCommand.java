package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.command.OrganizationCommand;

/**
 * Type for school-organization students groups management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.StudentsGroup
 */
public interface StudentsGroupCommand<T> extends OrganizationCommand<T> {
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "groupCommandsFactory";
    /**
     * Command-ID: for find all students groups
     */
    String FIND_ALL = "organization.students.group.findAll";
    /**
     * Command-ID: for find by ID students group
     */
    String FIND_BY_ID = "organization.students.group.findById";
    /**
     * Command-ID: for create or update students group entity
     */
    String CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    /**
     * Command-ID: for delete students group entity
     */
    String DELETE = "organization.students.group.delete";
}

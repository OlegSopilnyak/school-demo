package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

/**
 * Type for school-organization students groups management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.StudentsGroup
 */
public interface StudentsGroupCommand extends OrganizationCommand {
    String GROUP_WITH_ID_PREFIX = "Students Group with ID:";
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

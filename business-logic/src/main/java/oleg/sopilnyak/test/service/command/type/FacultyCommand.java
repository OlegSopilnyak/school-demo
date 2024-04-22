package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.command.OrganizationCommand;

/**
 * Type for school-organization faculties management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public interface FacultyCommand<T> extends OrganizationCommand<T> {
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
}

package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.composite.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

/**
 * Type for school-organization faculties management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public interface FacultyCommand extends OrganizationCommand {
    String FACULTY_WITH_ID_PREFIX = "Faculty with ID:";
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

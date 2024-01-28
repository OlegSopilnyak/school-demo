package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of faculty command-ids
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public interface FacultyCommands {
    String FIND_ALL = "organization.faculty.findAll";
    String FIND_BY_ID = "organization.faculty.findById";
    String CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    String DELETE = "organization.faculty.delete";
}

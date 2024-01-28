package oleg.sopilnyak.test.service.command.id.set;


/**
 * Service-Command-Ids: The set of students-group command-ids
 * @see oleg.sopilnyak.test.school.common.model.StudentsGroup
 */
public interface StudentsGroupCommands {
    String FIND_ALL = "organization.students.group.findAll";
    String FIND_BY_ID = "organization.students.group.findById";
    String CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    String DELETE = "organization.students.group.delete";
}

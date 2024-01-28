package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of student-command-ids
 * @see oleg.sopilnyak.test.school.common.model.Student
 */
public interface StudentCommands {
    String FIND_BY_ID = "student.findById";
    String FIND_ENROLLED = "student.findEnrolledTo";
    String FIND_NOT_ENROLLED = "student.findNotEnrolled";
    String CREATE_OR_UPDATE = "student.createOrUpdate";
    String DELETE = "student.delete";
}

package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of course-command-ids
 *
 * @see oleg.sopilnyak.test.school.common.model.Course
 */
public interface CourseCommands {
    String FIND_BY_ID = "course.findById";
    String FIND_REGISTERED = "course.findRegisteredFor";
    String FIND_NOT_REGISTERED = "course.findWithoutStudents";
    String CREATE_OR_UPDATE = "course.createOrUpdate";
    String DELETE = "course.delete";
    String REGISTER = "course.register";
    String UN_REGISTER = "course.unRegister";
}

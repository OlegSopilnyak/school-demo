package oleg.sopilnyak.test.service.facade.course;

import oleg.sopilnyak.test.school.common.facade.CoursesFacade;

/**
 * Service-Facade: Service for manage students commands
 */
public interface CourseCommandsFacade extends CoursesFacade {
    String FIND_BY_ID = "course.findById";
    String FIND_REGISTERED = "course.findRegisteredFor";
    String FIND_NOT_REGISTERED = "course.findWithoutStudents";
    String CREATE_OR_UPDATE = "course.createOrUpdate";
    String DELETE = "course.delete";
    String REGISTER = "course.register";
    String UN_REGISTER = "course.unRegister";
}

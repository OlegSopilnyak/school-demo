package oleg.sopilnyak.test.service.facade.student;

import oleg.sopilnyak.test.school.common.facade.StudentsFacade;

/**
 * Service-Facade: Service for manage students commands
 */
public interface StudentCommandFacade extends StudentsFacade {
    String FIND_BY_ID = "student.findById";
    String FIND_ENROLLED = "student.findEnrolledTo";
    String FIND_NOT_ENROLLED = "student.findNotEnrolled";
    String CREATE_OR_UPDATE = "student.createOrUpdate";
    String DELETE = "student.delete";
}

package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;

/**
 * Persistence facade for students-course entities linking
 */
public interface StudentCourseLinkPersistenceFacade extends
        CoursesPersistenceFacade,
        StudentsPersistenceFacade,
        RegisterPersistenceFacade {
}

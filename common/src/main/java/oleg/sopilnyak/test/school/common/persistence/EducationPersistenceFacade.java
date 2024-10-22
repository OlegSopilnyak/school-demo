package oleg.sopilnyak.test.school.common.persistence;

import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;

/**
 * Persistence facade for students-course entities linking
 */
public interface EducationPersistenceFacade extends
        CoursesPersistenceFacade,
        StudentsPersistenceFacade,
        RegisterPersistenceFacade {
}

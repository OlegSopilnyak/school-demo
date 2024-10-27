package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.sql.implementation.students.courses.CoursesPersistence;
import oleg.sopilnyak.test.persistence.sql.implementation.students.courses.RegisterPersistenceFacadeImplementation;
import oleg.sopilnyak.test.persistence.sql.implementation.students.courses.StudentsPersistence;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;

/**
 * Persistence facade implementation for students-course entities linking
 */
public interface StudentCourseLinkPersistenceFacadeImplementation extends EducationPersistenceFacade,
        StudentsPersistence,
        CoursesPersistence,
        RegisterPersistenceFacadeImplementation {
}

package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.sql.implementation.education.CoursesPersistence;
import oleg.sopilnyak.test.persistence.sql.implementation.education.RegisterPersistence;
import oleg.sopilnyak.test.persistence.sql.implementation.education.StudentsPersistence;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;

/**
 * Persistence facade implementation for students-course entities linking
 */
public interface EducationPersistence extends EducationPersistenceFacade,
        StudentsPersistence, CoursesPersistence, RegisterPersistence {
}

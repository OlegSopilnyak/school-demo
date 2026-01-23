package oleg.sopilnyak.test.school.common.persistence.education.joint;

import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;

/**
 * Persistence facade for entities related to education process
 */
public interface EducationPersistenceFacade
        extends CoursesPersistenceFacade, StudentsPersistenceFacade, RegisterPersistenceFacade {
}

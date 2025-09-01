package oleg.sopilnyak.test.service.command.executable.education;

import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.service.command.io.parameter.PairParameter;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

/**
 * Common methods for Link/Unlink commands
 */
public interface EducationLinkCommand {
    String LINK_STUDENT_WITH_ID_PREFIX = "Student with ID:";
    String LINK_COURSE_WITH_ID_PREFIX = "Course with ID:";
    String IS_NOT_EXISTS_SUFFIX = " is not exists.";

    BusinessMessagePayloadMapper getPayloadMapper();

    EducationPersistenceFacade getPersistenceFacade();

    /**
     * To retrieve student by id from persistence
     *
     * @param input - input parameter with studentId in first position
     * @return found student
     * @throws StudentNotFoundException when student with provided id is not found
     */
    default Student retrieveStudent(final PairParameter<Long> input) {
        final Long studentId = input.first();
        return getPersistenceFacade().findStudentById(studentId)
                .orElseThrow(
                        () -> new StudentNotFoundException(LINK_STUDENT_WITH_ID_PREFIX + studentId + IS_NOT_EXISTS_SUFFIX)
                );
    }

    /**
     * To retrieve course by id from persistence
     *
     * @param input - input parameter with courseId in second position
     * @return found course
     * @throws CourseNotFoundException when course with provided id is not found
     */
    default Course retrieveCourse(final PairParameter<Long> input) {
        final Long courseId = input.second();
        return getPersistenceFacade().findCourseById(courseId)
                .orElseThrow(
                        () -> new CourseNotFoundException(LINK_COURSE_WITH_ID_PREFIX + courseId + IS_NOT_EXISTS_SUFFIX)
                );
    }
}

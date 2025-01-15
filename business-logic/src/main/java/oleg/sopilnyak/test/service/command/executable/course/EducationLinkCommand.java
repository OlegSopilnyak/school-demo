package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.service.command.io.parameter.PairParameter;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

/**
 * Common methods for Link/Unlink commands
 */
public interface EducationLinkCommand {
    String LINK_STUDENT_WITH_ID_PREFIX = "Student with ID:";
    String LINK_COURSE_WITH_ID_PREFIX = "Course with ID:";
    String IS_NOT_EXISTS_SUFFIX = " is not exists.";

    BusinessMessagePayloadMapper getPayloadMapper();
    EducationPersistenceFacade getPersistenceFacade();

    default Student detached(Student student) {
        return student instanceof StudentPayload payload ? payload : getPayloadMapper().toPayload(student);
    }

    default Course detached(Course course) {
        return course instanceof CoursePayload payload ? payload : getPayloadMapper().toPayload(course);
    }
    default Student retrieveStudent(final PairParameter<Long> input) {
        final Long studentId = input.first();
        return getPersistenceFacade().findStudentById(studentId)
                .orElseThrow(
                        () -> new StudentNotFoundException(LINK_STUDENT_WITH_ID_PREFIX + studentId + IS_NOT_EXISTS_SUFFIX)
                );
    }
    default Course retrieveCourse(final PairParameter<Long> input) {
        final Long courseId = input.second();
        return getPersistenceFacade().findCourseById(courseId)
                .orElseThrow(
                        () -> new CourseNotFoundException(LINK_COURSE_WITH_ID_PREFIX + courseId + IS_NOT_EXISTS_SUFFIX)
                );
    }
}

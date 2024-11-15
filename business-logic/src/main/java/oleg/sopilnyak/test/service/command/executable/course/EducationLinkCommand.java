package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

/**
 * Common methods for Link/Unlink commands
 */
public interface EducationLinkCommand {
    BusinessMessagePayloadMapper getPayloadMapper();

    default Student detached(Student student) {
        return student instanceof StudentPayload payload ? payload : getPayloadMapper().toPayload(student);
    }

    default Course detached(Course course) {
        return course instanceof CoursePayload payload ? payload : getPayloadMapper().toPayload(course);
    }
}

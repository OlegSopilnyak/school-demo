package oleg.sopilnyak.test.endpoint.rest.education;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.REGISTER)
@ResponseStatus(HttpStatus.OK)
public class RegisterCourseController {
    public static final String FACADE_NAME = "CoursesFacade";
    // delegate for requests processing
    private final CoursesFacade coursesFacade;
    private final StudentsFacade studentsFacade;

    @PutMapping("/{studentId}/to/{courseId}")
    public void registerToCourse(@PathVariable("studentId") String strStudentId,
                                 @PathVariable("courseId") String strCourseId) {
        ActionContext.setup(FACADE_NAME, "register");

        final Student student = restoreEntity(strStudentId, studentsFacade);
        final Course course = restoreEntity(strCourseId, coursesFacade);
        final long studentId = student.getId();
        final long courseId = course.getId();

        try {
            log.debug("Registering student: {} to course: {}", student, course);

            coursesFacade.register(student, course);

            log.debug("Linked student-id: {} to course-id: {}", studentId, courseId);
        } catch (CourseHasNoRoomException e) {
            throw new CannotProcessActionException("No room for student: " + studentId + " in course: " + courseId, e);
        } catch (StudentCoursesExceedException e) {
            throw new CannotProcessActionException("Too many courses for student:" + studentId, e);
        } catch (Exception e) {
            log.error("Cannot register student: {} to course: {}", studentId, courseId, e);
            throw new CannotProcessActionException("Cannot register student: " + studentId + " to course: " + courseId, e);
        }
    }

    @DeleteMapping("/{studentId}/to/{courseId}")
    public void unRegisterCourse(@PathVariable("studentId") String strStudentId,
                                 @PathVariable("courseId") String strCourseId) {
        ActionContext.setup(FACADE_NAME, "unRegister");
        final Student student = restoreEntity(strStudentId, studentsFacade);
        final Course course = restoreEntity(strCourseId, coursesFacade);
        final long studentId = student.getId();
        final long courseId = course.getId();

        try {
            log.debug("Un-Registering student: {} to course: {}", student, course);

            coursesFacade.unRegister(student, course);

            log.debug("Un-Registered student: {} to course: {}", studentId, courseId);
        } catch (Exception e) {
            log.error("Cannot un-register student: {} from course: {}", studentId, courseId, e);
            throw new CannotProcessActionException("Cannot un-register student: " + studentId + " from course: " + courseId, e);
        }
    }

    // private methods
    private static Student restoreEntity(final String strId, final StudentsFacade owner) {
        try {
            final Long entityId = Long.parseLong(strId);
            return owner.findById(entityId)
                    .orElseThrow(() -> new StudentNotFoundException("Student with id: " + entityId + " not found"));
        } catch (NumberFormatException | NullPointerException e) {
            log.error("Wrong student-id: '{}'", strId);
            throw new StudentNotFoundException("Wrong student-id: '" + strId + "'");
        }
    }

    private static Course restoreEntity(final String strId, final CoursesFacade owner) {
        try {
            final Long entityId = Long.parseLong(strId);
            return owner.findById(entityId)
                    .orElseThrow(() -> new CourseNotFoundException("Course with id: " + entityId + " not found"));
        } catch (NumberFormatException | NullPointerException e) {
            log.error("Wrong course-id: '{}'", strId);
            throw new CourseNotFoundException("Wrong course-id: '" + strId + "'");
        }
    }

}

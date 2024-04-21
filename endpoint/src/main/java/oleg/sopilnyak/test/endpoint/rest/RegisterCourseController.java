package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.exception.CannotDoRestCallException;
import oleg.sopilnyak.test.endpoint.exception.CannotRegisterToCourseException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.NoRoomInTheCourseException;
import oleg.sopilnyak.test.school.common.exception.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.business.CoursesFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/register")
public class RegisterCourseController {
    public static final String IS_NOT_EXIST_PHRASE = " is not exist";
    // delegate for requests processing
    private final CoursesFacade facade;

    @PutMapping("/{studentId}/to/{courseId}")
    public ResponseEntity<Void> registerCourse(
            @PathVariable("studentId") String strStudentId,
            @PathVariable("courseId") String strCourseId) {
        Long studentId = restoreId(strStudentId, "student");
        Long courseId = restoreId(strCourseId, "course");

        try {
            log.debug("Registering student: {} to course: {}", studentId, courseId);
            facade.register(studentId, courseId);
            log.debug("Registered student: {} to course: {}", studentId, courseId);
            return ResponseEntity.ok().build();
        } catch (StudentNotExistsException e) {
            log.error("Student with id: {} is not exist", studentId);
            throw new ResourceNotFoundException("Student with id: " + studentId + IS_NOT_EXIST_PHRASE);
        } catch (CourseNotExistsException e) {
            log.error("Course with id: {} is not exist", courseId);
            throw new ResourceNotFoundException("Course with id: " + courseId + IS_NOT_EXIST_PHRASE);
        } catch (NoRoomInTheCourseException e) {
            log.error("No room for student: {} in course: {}", studentId, courseId);
            throw new CannotRegisterToCourseException("No room for student: " + studentId + " in course: " + courseId, e);
        } catch (StudentCoursesExceedException e) {
            log.error("Too many courses for student: {}", studentId);
            throw new CannotRegisterToCourseException("Too many courses for student:" + studentId, e);
        } catch (Exception e) {
            log.error("Cannot register student: {} to course: {}", studentId, courseId, e);
            throw new CannotDoRestCallException("Cannot register student: " + studentId + " to course: " + courseId, e);
        }
    }

    @DeleteMapping("/{studentId}/to/{courseId}")
    public ResponseEntity<Void> unRegisterCourse(
            @PathVariable("studentId") String strStudentId,
            @PathVariable("courseId") String strCourseId) {
        Long studentId = restoreId(strStudentId, "student");
        Long courseId = restoreId(strCourseId, "course");

        try {
            log.debug("Un-Registering student: {} to course: {}", studentId, courseId);
            facade.unRegister(studentId, courseId);
            log.debug("Un-Registered student: {} to course: {}", studentId, courseId);
            return ResponseEntity.ok().build();
        } catch (StudentNotExistsException e) {
            log.error("Student with id: {} is not exist", studentId);
            throw new ResourceNotFoundException("Student with id: " + studentId + IS_NOT_EXIST_PHRASE);
        } catch (CourseNotExistsException e) {
            log.error("Course with id: {} is not exist", courseId);
            throw new ResourceNotFoundException("Course with id: " + courseId + IS_NOT_EXIST_PHRASE);
        } catch (Exception e) {
            log.error("Cannot un-register student: {} from course: {}", studentId, courseId, e);
            throw new CannotDoRestCallException("Cannot un-register student: " + studentId + " from course: " + courseId, e);
        }
    }

    private static Long restoreId(String strId, String owns) {
        try {
            return Long.parseLong(strId);
        } catch (NumberFormatException | NullPointerException e) {
            log.error("Wrong {}-id: '{}'", owns, strId);
            throw new ResourceNotFoundException("Wrong " + owns + "-id: '" + strId + "'");
        }
    }

}

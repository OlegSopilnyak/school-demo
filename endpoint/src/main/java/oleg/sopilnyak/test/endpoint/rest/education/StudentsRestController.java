package oleg.sopilnyak.test.endpoint.rest.education;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.STUDENTS)
@ResponseStatus(HttpStatus.OK)
public class StudentsRestController {
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);
    public static final String COURSE_ID_VAR_NAME = "courseId";
    public static final String STUDENT_ID_VAR_NAME = "studentId";
    public static final String WRONG_STUDENT_ID_EXCEPTION = "Wrong student-id: '";

    public static final String FACADE_NAME = "StudentsFacade";
    // delegate for requests processing
    private final StudentsFacade facade;

    @GetMapping("/{" + STUDENT_ID_VAR_NAME + "}")
    public StudentDto findStudent(@PathVariable(STUDENT_ID_VAR_NAME) String studentId) {
        ActionContext.setup(FACADE_NAME, "findById");
        log.debug("Trying to get student by Id: '{}'", studentId);
        try {
            final long id = Long.parseLong(studentId);
            log.debug("Getting student for id: {}", id);

            return resultToDto(studentId, facade.findById(id));
        } catch (NumberFormatException | StudentNotFoundException e) {
            throw new StudentNotFoundException(WRONG_STUDENT_ID_EXCEPTION + studentId + "'");
        } catch (Exception e) {
            log.error("Cannot get student for id: {}", studentId);
            throw new CannotProcessActionException("Cannot get student for id: " + studentId, e);
        }
    }

    @GetMapping("/enrolled/{" + COURSE_ID_VAR_NAME + "}")
    public List<StudentDto> findEnrolledTo(@PathVariable(COURSE_ID_VAR_NAME) String courseId) {
        ActionContext.setup(FACADE_NAME, "findEnrolledTo");
        log.debug("Trying to get students for course Id: '{}'", courseId);
        try {
            final long id = Long.parseLong(courseId);
            log.debug("Getting students for course Id: {}", id);

            return resultToDto(facade.findEnrolledTo(id));
        } catch (NumberFormatException e) {
            throw new CourseNotFoundException("Wrong course id: '" + courseId + "'");
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot get courses for student-id: " + courseId, e);
        }
    }

    @GetMapping("/empty")
    public List<StudentDto> findNotEnrolledStudents() {
        ActionContext.setup(FACADE_NAME, "findNotEnrolled");
        log.debug("Trying to get not enrolled students");
        try {
            return resultToDto(facade.findNotEnrolled());
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot get not enrolled students");
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDto createStudent(@RequestBody StudentDto studentDto) {
        ActionContext.setup(FACADE_NAME, "createNew");
        log.debug("Trying to create the student {}", studentDto);
        try {
            return resultToDto(facade.create(studentDto));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot create the student: " + studentDto.toString());
        }
    }

    @PutMapping
    public StudentDto updateStudent(@RequestBody StudentDto studentDto) {
        ActionContext.setup(FACADE_NAME, "createOrUpdate");
        log.debug("Trying to update the student {}", studentDto);
        try {
            final Long id = studentDto.getId();
            if (isInvalid(id)) {
                throw new StudentNotFoundException(WRONG_STUDENT_ID_EXCEPTION + id + "'");
            }
            return resultToDto(facade.createOrUpdate(studentDto));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot update student " + studentDto, e);
        }
    }

    @DeleteMapping("/{" + STUDENT_ID_VAR_NAME + "}")
    public void deleteStudent(@PathVariable(STUDENT_ID_VAR_NAME) String studentId) {
        ActionContext.setup(FACADE_NAME, "deleteById");
        log.debug("Trying to delete student by Id: '{}'", studentId);
        try {
            final long id = Long.parseLong(studentId);
            log.debug("Deleting student for id: {}", id);

            facade.delete(id);
        } catch (NumberFormatException | StudentNotFoundException e) {
            throw new StudentNotFoundException(WRONG_STUDENT_ID_EXCEPTION + studentId + "'");
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot delete student for id = " + studentId, e);
        }
    }

    // private methods
    private StudentDto resultToDto(Optional<Student> student) {
        log.debug("Converting {} to DTO", student);
        return mapper.toDto(
                student.orElseThrow(() -> new StudentNotFoundException("Student is not updated"))
        );
    }

    private List<StudentDto> resultToDto(Set<Student> registeredFor) {
        return registeredFor.stream().map(mapper::toDto).filter(Objects::nonNull)
                .sorted(Comparator.comparing(StudentDto::getFullName))
                .toList();
    }

    private StudentDto resultToDto(String studentId, Optional<Student> student) {
        log.debug("Converting {} to DTO for student-id '{}'", student, studentId);
        return mapper.toDto(
                student.orElseThrow(() -> new StudentNotFoundException("Student with id: " + studentId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0L;
    }
}

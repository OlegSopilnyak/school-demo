package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.STUDENTS)
public class StudentsRestController {
    public static final String COURSE_VAR_NAME = "courseId";
    public static final String STUDENT_VAR_NAME = "studentId";
    // delegate for requests processing
    private final StudentsFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping("/{" + STUDENT_VAR_NAME + "}")
    public ResponseEntity<StudentDto> findStudent(@PathVariable(STUDENT_VAR_NAME) String studentId) {
        log.debug("Trying to get student by Id: '{}'", studentId);
        try {
            Long id = Long.parseLong(studentId);
            log.debug("Getting student for id: {}", id);

            return ResponseEntity.ok(resultToDto(studentId, facade.findById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong student-id: '{}'", studentId);
            throw new ResourceNotFoundException("Wrong student-id: '" + studentId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get student for id: " + studentId, e);
        }
    }

    @GetMapping("/enrolled/{" + COURSE_VAR_NAME + "}")
    public ResponseEntity<List<StudentDto>> findEnrolledTo(@PathVariable(COURSE_VAR_NAME) String courseId) {
        log.debug("Trying to get students for course Id: '{}'", courseId);
        try {
            Long id = Long.parseLong(courseId);
            log.debug("Getting student for id: {}", id);

            return ResponseEntity.ok(resultToDto(facade.findEnrolledTo(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong student-id: '{}'", courseId);
            throw new ResourceNotFoundException("Wrong student-id: '" + courseId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get courses for student-id: " + courseId, e);
        }
    }

    @GetMapping("/empty")
    public ResponseEntity<List<StudentDto>> findNotEnrolledStudents() {
        log.debug("Trying to get not enrolled students");
        try {
            return ResponseEntity.ok(resultToDto(facade.findNotEnrolled()));
        } catch (Exception e) {
            throw new RuntimeException("Cannot get not enrolled students");
        }
    }

    @PostMapping
    public ResponseEntity<StudentDto> createStudent(@RequestBody StudentDto newStudent) {
        log.debug("Trying to create the student {}", newStudent);
        try {
            return ResponseEntity.ok(resultToDto(facade.createOrUpdate(newStudent)));
        } catch (Exception e) {
            throw new RuntimeException("Cannot create new Student " + newStudent.toString());
        }
    }

    @PutMapping
    public ResponseEntity<StudentDto> updateStudent(@RequestBody StudentDto student) {
        log.debug("Trying to update the student {}", student);
        try {
            Long id = student.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException("Wrong student-id: '" + id + "'");
            }
            return ResponseEntity.ok(resultToDto(facade.createOrUpdate(student)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot update student " + student.toString());
        }
    }

    @DeleteMapping("/{" + STUDENT_VAR_NAME + "}")
    public ResponseEntity<Void> deleteStudent(@PathVariable(STUDENT_VAR_NAME) String studentId) {
        log.debug("Trying to delete student by Id: '{}'", studentId);
        try {
            Long id = Long.parseLong(studentId);
            log.debug("Deleting student for id: {}", id);

            facade.delete(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | StudentNotExistsException e) {
            log.error("Wrong student-id: '{}'", studentId);
            throw new ResourceNotFoundException("Wrong student-id: '" + studentId + "'");
        } catch (Exception e) {
            log.error("Cannot delete student for id = {}", studentId, e);
            throw new CannotDeleteResourceException("Cannot delete student for id = " + studentId, e);
        }
    }

    // private methods
    private StudentDto resultToDto(Optional<Student> student) {
        log.debug("Converting {} to DTO", student);
        return mapper.toDto(
                student.orElseThrow(() -> new ResourceNotFoundException("Student is not updated"))
        );
    }

    private List<StudentDto> resultToDto(Set<Student> registeredFor) {
        return registeredFor.stream()
                .map(mapper::toDto)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StudentDto::getFullName))
                .toList();
    }

    private StudentDto resultToDto(String studentId, Optional<Student> student) {
        log.debug("Converting {} to DTO for student-id '{}'", student, studentId);
        return mapper.toDto(
                student.orElseThrow(() -> new ResourceNotFoundException("Student with id: " + studentId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }


}

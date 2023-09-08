package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.COURSES)
public class CoursesRestController {
    public static final String COURSE_VAR_NAME = "courseId";
    public static final String STUDENT_VAR_NAME = "studentId";
    // delegate for requests processing
    private final CoursesFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping("/{" + COURSE_VAR_NAME + "}")
    public ResponseEntity<CourseDto> findCourse(@PathVariable(COURSE_VAR_NAME) String courseId) {
        log.debug("Trying to get course by Id: '{}'", courseId);
        try {
            Long id = Long.parseLong(courseId);
            log.debug("Getting course for id: {}", id);

            return ResponseEntity.ok(resultToDto(courseId, facade.findById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong course-id: '{}'", courseId);
            throw new ResourceNotFoundException("Wrong course-id: '" + courseId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get course for id: " + courseId, e);
        }
    }

    @GetMapping("/registered/{" + STUDENT_VAR_NAME + "}")
    public ResponseEntity<List<CourseDto>> findRegisteredFor(@PathVariable(STUDENT_VAR_NAME) String studentId) {
        log.debug("Trying to get courses for student Id: '{}'", studentId);
        try {
            Long id = Long.parseLong(studentId);
            log.debug("Getting course for id: {}", id);

            return ResponseEntity.ok(resultToDto(facade.findRegisteredFor(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong student-id: '{}'", studentId);
            throw new ResourceNotFoundException("Wrong student-id: '" + studentId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get courses for student-id: " + studentId, e);
        }
    }

    @GetMapping("/empty")
    public ResponseEntity<List<CourseDto>> findEmptyCourses() {
        log.debug("Trying to get empty courses");
        try {
            return ResponseEntity.ok(resultToDto(facade.findWithoutStudents()));
        } catch (Exception e) {
            throw new RuntimeException("Cannot get empty courses", e);
        }
    }

    @PostMapping
    public ResponseEntity<CourseDto> createCourse(@RequestBody CourseDto newCourse) {
        log.debug("Trying to create the course {}", newCourse);
        try {
            return ResponseEntity.ok(resultToDto(facade.createOrUpdate(newCourse)));
        } catch (Exception e) {
            throw new RuntimeException("Cannot create new course " + newCourse.toString());
        }
    }

    @PutMapping
    public ResponseEntity<CourseDto> updateCourse(@RequestBody CourseDto courseDto) {
        log.debug("Trying to update the course {}", courseDto);
        try {
            Long id = courseDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException("Wrong course-id: '" + id + "'");
            }
            return ResponseEntity.ok(resultToDto(facade.createOrUpdate(courseDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot update the course " + courseDto.toString(), e);
        }
    }

    @DeleteMapping("/{" + COURSE_VAR_NAME + "}")
    public ResponseEntity<Void> deleteCourse(@PathVariable(COURSE_VAR_NAME) String courseId) {
        log.debug("Trying to delete course for Id: '{}'", courseId);
        try {
            Long id = Long.parseLong(courseId);
            log.debug("Deleting course for id: {}", id);

            facade.delete(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | CourseNotExistsException e) {
            log.error("Wrong course-id: '{}'", courseId);
            throw new ResourceNotFoundException("Wrong course-id: '" + courseId + "'");
        } catch (Exception e) {
            log.error("Cannot delete course for id = {}", courseId, e);
            throw new CannotDeleteResourceException("Cannot delete course for id = " + courseId, e);
        }
    }

    private CourseDto resultToDto(Optional<Course> course) {
        log.debug("Converting {} to DTO", course);
        return mapper.toDto(
                course.orElseThrow(() -> new ResourceNotFoundException("Course is not updated"))
        );
    }

    private List<CourseDto> resultToDto(Set<Course> registeredFor) {
        return registeredFor.stream()
                .map(mapper::toDto)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CourseDto::getName))
                .toList();
    }

    private CourseDto resultToDto(String courseId, Optional<Course> course) {
        log.debug("Converting {} to DTO for course-id '{}'", course, courseId);
        return mapper.toDto(
                course.orElseThrow(() -> new ResourceNotFoundException("Course with id: " + courseId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }
}

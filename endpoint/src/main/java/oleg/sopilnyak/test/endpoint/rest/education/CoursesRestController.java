package oleg.sopilnyak.test.endpoint.rest.education;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.education.CourseDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.COURSES)
@ResponseStatus(HttpStatus.OK)
public class CoursesRestController {
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    public static final String COURSE_VAR_NAME = "courseId";
    public static final String STUDENT_VAR_NAME = "studentId";
    public static final String WRONG_COURSE_ID = "Wrong course-id: '";

    public static final String FACADE_NAME = "CoursesFacade";
    // delegate for requests processing
    private final CoursesFacade facade;

    @GetMapping("/{" + COURSE_VAR_NAME + "}")
    public CourseDto findCourse(@PathVariable(COURSE_VAR_NAME) String courseId) {
        ActionContext.setup(FACADE_NAME, "findById");
        log.debug("Trying to get course by Id: '{}'", courseId);
        try {
            final long id = Long.parseLong(courseId);
            log.debug("Getting course for id: {}", id);

            return resultToDto(courseId, facade.findById(id));
        } catch (NumberFormatException | CourseNotFoundException e) {
            throw new CourseNotFoundException(WRONG_COURSE_ID + courseId + "'", e);
        } catch (Exception e) {
            throw new CannotProcessActionException(e);
        }
    }

    @GetMapping("/registered/{" + STUDENT_VAR_NAME + "}")
    public List<CourseDto> findRegisteredFor(@PathVariable(STUDENT_VAR_NAME) String studentId) {
        ActionContext.setup(FACADE_NAME, "findRegisteredFor");
        log.debug("Trying to get courses for student Id: '{}'", studentId);
        try {
            final long id = Long.parseLong(studentId);
            log.debug("Getting courses for student Id: '{}'", studentId);

            return resultToDto(facade.findRegisteredFor(id));
        } catch (NumberFormatException e) {
            throw new StudentNotFoundException("Wrong student-id: '" + studentId + "'");
        } catch (Exception e) {
            throw new CannotProcessActionException(e);
        }
    }

    @GetMapping("/empty")
    public List<CourseDto> findEmptyCourses() {
        ActionContext.setup(FACADE_NAME, "findWithoutStudents");
        log.debug("Trying to get empty courses");
        try {
            return resultToDto(facade.findWithoutStudents());
        } catch (Exception e) {
            throw new CannotProcessActionException(e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDto createCourse(@RequestBody CourseDto courseDto) {
        ActionContext.setup(FACADE_NAME, "createNew");
        log.debug("Trying to create the course {}", courseDto);
        try {
            courseDto.setId(null);
            return resultToDto("new-id", facade.createOrUpdate(courseDto));
        } catch (Exception e) {
            throw new CannotProcessActionException(e);
        }
    }

    @PutMapping
    public CourseDto updateCourse(@RequestBody CourseDto courseDto) {
        ActionContext.setup(FACADE_NAME, "createOrUpdate");
        log.debug("Trying to update the course {}", courseDto);
        try {
            final Long id = courseDto.getId();
            if (isInvalid(id)) {
                throw new CourseNotFoundException(WRONG_COURSE_ID + id + "'");
            }

            return resultToDto(String.valueOf(id), facade.createOrUpdate(courseDto));
        } catch (Exception e) {
            throw new CannotProcessActionException(e);
        }
    }

    @DeleteMapping("/{" + COURSE_VAR_NAME + "}")
    public void deleteCourse(@PathVariable(COURSE_VAR_NAME) String courseId) {
        ActionContext.setup(FACADE_NAME, "deleteById");
        log.debug("Trying to delete course for Id: '{}'", courseId);
        try {
            final long id = Long.parseLong(courseId);
            log.debug("Deleting course for id: {}", id);
            if (isInvalid(id)) {
                throw new CourseNotFoundException(WRONG_COURSE_ID + id + "'");
            }

            facade.delete(id);

        } catch (NumberFormatException | CourseNotFoundException e) {
            throw new CourseNotFoundException(WRONG_COURSE_ID + courseId + "'");
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot delete course for id: " + courseId, e);
        }
    }

    // private methods
    private List<CourseDto> resultToDto(Set<Course> registeredFor) {
        return registeredFor.stream().map(mapper::toDto)
                .filter(Objects::nonNull).sorted(Comparator.comparing(CourseDto::getName))
                .toList();
    }

    private CourseDto resultToDto(String courseId, Optional<Course> course) {
        log.debug("Converting {} to DTO for course-id '{}'", course, courseId);
        return mapper.toDto(
                course.orElseThrow(() -> new CourseNotFoundException("Course with id: " + courseId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0L;
    }

}

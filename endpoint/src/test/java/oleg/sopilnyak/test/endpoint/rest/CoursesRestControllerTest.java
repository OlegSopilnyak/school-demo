package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class CoursesRestControllerTest extends TestModelFactory {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    CoursesFacade facade;
    @Spy
    @InjectMocks
    CoursesRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    void shouldFindCourse() throws Exception {
        Long id = 100L;
        Course course = makeTestCourse(id);
        when(facade.findById(id)).thenReturn(Optional.of(course));
        String requestPath = "/courses/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findCourse(id.toString());
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(id).isEqualTo(courseDto.getId());
        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
    }

    @Test
    void shouldFindEnrolledFor() throws Exception {
        Long studentId = 200L;
        Long coursesAmount = 10L;
        Set<Course> courses = LongStream.range(0, coursesAmount).mapToObj(this::makeTestCourse).collect(Collectors.toSet());
        when(facade.findRegisteredFor(studentId)).thenReturn(courses);
        String requestPath = "/courses/registered/" + studentId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findRegisteredFor(studentId.toString());
        String responseString = result.getResponse().getContentAsString();
        List<Course> courseDtos = MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
        }).stream().map(course -> (Course)course).toList();

        assertThat(courseDtos).hasSize(coursesAmount.intValue());
        assertCourseLists(courses.stream().toList(), courseDtos);
    }

    @Test
    void shouldFindEmptyCourses() throws Exception {
        Long coursesAmount = 5L;
        Set<Course> courses = LongStream.range(0, coursesAmount).mapToObj(this::makeTestCourse).collect(Collectors.toSet());
        when(facade.findWithoutStudents()).thenReturn(courses);
        String requestPath = "/courses/empty";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findEmptyCourses();
        String responseString = result.getResponse().getContentAsString();
        List<Course> courseDtos = MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
        }).stream().map(course -> (Course)course).toList();

        assertThat(courseDtos).hasSize(coursesAmount.intValue());
        assertCourseLists(courses.stream().toList(), courseDtos);
    }

    @Test
    void shouldCreateCourse() throws Exception {
        Course course = makeTestCourse(null);
        doAnswer(invocation -> {
            Course received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertCourseEquals(course, received);
            assertStudentLists(course.getStudents(), received.getStudents());
            return Optional.of(course);
        }).when(facade).createOrUpdate(any(Course.class));
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).createCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
    }

    @Test
    void shouldUpdateValidCourse() throws Exception {
        Long id = 101L;
        Course course = makeTestCourse(id);
        doAnswer(invocation -> {
            Course received = invocation.getArgument(0);
            assertThat(id).isEqualTo(received.getId());
            assertCourseEquals(course, received);
            assertStudentLists(course.getStudents(), received.getStudents());
            return Optional.of(course);
        }).when(facade).createOrUpdate(any(Course.class));
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
    }

    @Test
    void shouldNotUpdateInvalidCourse_NullId() throws Exception {
        Course course = makeTestCourse(null);
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: 'null'");
    }

    @Test
    void shouldNotUpdateInvalidCourse_NegativeId() throws Exception {
        Long id = -101L;
        Course course = makeTestCourse(id);
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '-101'");
    }

    @Test
    void shouldDeleteCourseValidId() throws Exception {
        Long id = 102L;
        String requestPath = "/courses/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteCourse(id.toString());
    }

    @Test
    void shouldDeleteCourseValidId_CourseNotExistsException() throws Exception {
        Long id = 103L;
        String requestPath = "/courses/" + id;
        doThrow(new CourseNotExistsException("Wrong course")).when(facade).delete(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(id.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '103'");
    }

    @Test
    void shouldDeleteCourseValidId_CourseWithStudentsException() throws Exception {
        Long id = 104L;
        String requestPath = "/courses/" + id;
        doThrow(new CourseWithStudentsException("Wrong course")).when(facade).delete(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(id.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Cannot delete course for id = 104");
    }

}
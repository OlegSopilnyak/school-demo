package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
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
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class CoursesRestControllerTest {
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
        List<CourseDto> courseDtos = MAPPER.readValue(responseString, new TypeReference<>() {
        });

        assertThat(courseDtos).hasSize(coursesAmount.intValue());
        assertCoursesList(courses, courseDtos);
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
        List<CourseDto> courseDtos = MAPPER.readValue(responseString, new TypeReference<>() {
        });

        assertThat(courseDtos).hasSize(coursesAmount.intValue());
        assertCoursesList(courses, courseDtos);
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

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong course-id: 'null'").isEqualTo(error.getErrorMessage());
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

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong course-id: '-101'").isEqualTo(error.getErrorMessage());
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

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong course-id: '103'").isEqualTo(error.getErrorMessage());
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

        assertThat(409).isEqualTo(error.getErrorCode());
        assertThat("Cannot delete course for id = 104").isEqualTo(error.getErrorMessage());
    }

    private void assertCoursesList(Set<Course> expected, List<CourseDto> result) {
        result.forEach(dto -> assertCourseForSet(expected, dto));
    }

    private void assertCourseForSet(Set<Course> expected, CourseDto dto) {
        Optional<Course> courseExpected = expected.stream().filter(course -> course.getId().equals(dto.getId())).findFirst();
        assertThat(courseExpected).isNotEmpty();
        assertCourseEquals(dto, courseExpected.get());
    }

    private void assertStudentLists(List<Student> expected, List<Student> result) {
        if (ObjectUtils.isEmpty(expected)) {
            assertThat(ObjectUtils.isEmpty(result)).isTrue();
            return;
        }
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertStudentEquals(expected.get(i), result.get(i)));
    }

    private void assertCourseEquals(Course expected, Course result) {
        assertThat(expected.getId()).isEqualTo(result.getId());
        assertThat(expected.getName()).isEqualTo(result.getName());
        assertThat(expected.getDescription()).isEqualTo(result.getDescription());
    }

    private void assertStudentEquals(Student expected, Student result) {
        assertThat(expected.getId()).isEqualTo(result.getId());
        assertThat(expected.getFirstName()).isEqualTo(result.getFirstName());
        assertThat(expected.getLastName()).isEqualTo(result.getLastName());
        assertThat(expected.getGender()).isEqualTo(result.getGender());
        assertThat(expected.getDescription()).isEqualTo(result.getDescription());
    }

    private Course makeTestCourse(Long id) {
        String name = "courseName";
        String description = "description";
        List<Student> students = makeStudents(50);
        return FakeCourse.builder()
                .id(id).name(name).description(description).students(students)
                .build();
    }

    private List<Student> makeStudents(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeStudent(i + 1)).toList();
    }

    private Student makeStudent(int i) {
        return FakeStudent.builder()
                .id(i + 200L).firstName("firstName-" + i).lastName("lastName-" + i)
                .gender("gender-" + i).description("description-" + i)
                .courses(Collections.emptyList())
                .build();
    }

    @Data
    @Builder
    private static class FakeStudent implements Student {
        private Long id;
        private String firstName;
        private String lastName;
        private String gender;
        private String description;
        private List<Course> courses;
    }

    @Data
    @Builder
    private static class FakeCourse implements Course {
        private Long id;
        private String name;
        private String description;
        private List<Student> students;
    }
}
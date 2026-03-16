package oleg.sopilnyak.test.endpoint.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
class CoursesRestControllerTest extends TestModelFactory {
    private static final String COURSE_FIND_BY_ID = "school::education::courses:find.By.Id";
    private static final String COURSE_FIND_REGISTERED = "school::education::courses:find.Registered.To.The.Student";
    private static final String COURSE_FIND_NOT_REGISTERED = "school::education::courses:find.Without.Any.Student";
    private static final String COURSE_CREATE_OR_UPDATE = "school::education::courses:create.Or.Update";
    private static final String COURSE_DELETE = "school::education::courses:delete";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    CoursesFacade facade;
    @MockitoSpyBean
    @Autowired
    CoursesRestController controller;
    @MockitoSpyBean
    @Autowired
    AdviseDelegate delegate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    @WithMockUser(authorities = {"EDU_GET"})
    void shouldFindCourse() throws Exception {
        Long id = 100L;
        Course course = makeTestCourse(id);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(id);
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
        verify(facade).doActionAndResult(COURSE_FIND_BY_ID, id);
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(id).isEqualTo(courseDto.getId());
        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_LIST", "EDU_GET"})
    void shouldFindEnrolledFor() throws Exception {
        Long studentId = 200L;
        long coursesAmount = 10L;
        Set<Course> courses = LongStream.range(0, coursesAmount).mapToObj(this::makeTestCourse).collect(Collectors.toSet());
        doReturn(courses).when(persistenceFacade).findCoursesRegisteredForStudent(studentId);
        List<Course> selectedCoursesList = courses.stream().sorted(Comparator.comparing(Course::getId)).toList();
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
        verify(facade).doActionAndResult(COURSE_FIND_REGISTERED, studentId);
        String responseString = result.getResponse().getContentAsString();
        List<Course> courseList =
                MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
                }).stream().map(Course.class::cast).sorted(Comparator.comparing(Course::getId)).toList();

        assertThat(courseList).hasSameSizeAs(courses).hasSize((int) coursesAmount);
        assertCourseLists(selectedCoursesList, courseList);
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_LIST", "EDU_GET"})
    void shouldFindEmptyCourses() throws Exception {
        long coursesAmount = 5L;
        Set<Course> courses = LongStream.range(0, coursesAmount).mapToObj(this::makeTestCourse).collect(Collectors.toSet());
        doReturn(courses).when(persistenceFacade).findCoursesWithoutStudents();
        List<Course> selectedCoursesList = courses.stream().sorted(Comparator.comparing(Course::getId)).toList();
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
        verify(facade).doActionAndResult(COURSE_FIND_NOT_REGISTERED);
        String responseString = result.getResponse().getContentAsString();
        List<Course> courseList =
                MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
                }).stream().map(Course.class::cast).sorted(Comparator.comparing(Course::getId)).toList();

        assertThat(courseList).hasSameSizeAs(courses).hasSize((int) coursesAmount);
        assertCourseLists(selectedCoursesList, courseList);
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_CREATE", "EDU_GET"})
    void shouldCreateCourse() throws Exception {
        Course course = makeTestCourse(null);
        doAnswer(invocation -> {
            Course received = invocation.getArgument(1);
            assertThat(received.getId()).isNull();
            assertCourseEquals(course, received);
            assertStudentLists(course.getStudents(), received.getStudents());
            return Optional.of(course);
        }).when(facade).doActionAndResult(eq(COURSE_CREATE_OR_UPDATE), any(Course.class));
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).createCourse(any(CourseDto.class));
        verify(facade).doActionAndResult(eq(COURSE_CREATE_OR_UPDATE), any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_CREATE", "EDU_GET"})
    void shouldNotCreateCourse_CourseStudentsRepetition() throws Exception {
        Course course = makeTestCourse(null);
        if (course instanceof FakeCourse entity) {
            entity.setStudents(List.of(makeClearStudent(10),  makeClearStudent(10)));
        } else {
            fail("Wrong course type");
        }
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isBadRequest())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(error.getErrorMessage()).startsWith("Validation failed for argument").contains("Students should be unique");
        // check the behavior
        verify(controller, never()).createCourse(any(CourseDto.class));
    }

    @Test
    @WithMockUser(authorities = {"EDU_UPDATE", "EDU_GET"})
    void shouldUpdateValidCourse() throws Exception {
        Long id = 101L;
        Course course = makeTestCourse(id);
        doAnswer(invocation -> {
            Course received = invocation.getArgument(1);
            assertThat(id).isEqualTo(received.getId());
            assertCourseEquals(course, received);
            assertStudentLists(course.getStudents(), received.getStudents());
            return Optional.of(course);
        }).when(facade).doActionAndResult(eq(COURSE_CREATE_OR_UPDATE), any(Course.class));
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
        verify(facade).doActionAndResult(eq(COURSE_CREATE_OR_UPDATE), any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_UPDATE", "EDU_GET"})
    void shouldNotUpdateValidCourse_CourseStudentsRepetition() throws Exception {
        Long id = 101L;
        Course course = makeTestCourse(id);
        if (course instanceof FakeCourse entity) {
            entity.setStudents(List.of(makeClearStudent(20),  makeClearStudent(20)));
        } else {
            fail("Wrong course type");
        }
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isBadRequest())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(error.getErrorMessage()).startsWith("Validation failed for argument").contains("Students should be unique");
        // check the behavior
        verify(controller, never()).updateCourse(any(CourseDto.class));
    }

    @Test
    @WithMockUser(authorities = {"EDU_UPDATE", "EDU_GET"})
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
        verify(facade, never()).doActionAndResult(anyString(), any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: 'null'");
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_UPDATE", "EDU_GET"})
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
        verify(facade, never()).doActionAndResult(anyString(), any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '-101'");
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_DELETE"})
    void shouldDeleteCourseValidId() throws Exception {
        long id = 102L;
        doReturn(Optional.of(mock(Course.class))).when(persistenceFacade).findCourseById(id);
        String requestPath = "/courses/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteCourse(Long.toString(id));
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_DELETE"})
    void shouldDeleteCourseValidId_CourseNotExistsException() throws Exception {
        Long id = 103L;
        String requestPath = "/courses/" + id;
        var exception = new CourseNotFoundException("Wrong course");
        doThrow(exception).when(persistenceFacade).findCourseById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(id.toString());
        verify(facade).doActionAndResult(COURSE_DELETE, id);
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '103'");
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"EDU_DELETE"})
    void shouldNotDeleteCourseValidId_CourseWithStudentsException() throws Exception {
        Long id = 104L;
        String requestPath = "/courses/" + id;
        String errorMessage = "Wrong course";
        var exception = new CourseWithStudentsException(errorMessage);
        doThrow(exception).when(facade).doActionAndResult(COURSE_DELETE, id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(id.toString());
        verify(facade).doActionAndResult(COURSE_DELETE, id);
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(CoursesRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(CoursesRestController.class);
    }
}

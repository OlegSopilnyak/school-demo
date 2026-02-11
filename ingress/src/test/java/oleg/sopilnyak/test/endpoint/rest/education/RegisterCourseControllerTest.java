package oleg.sopilnyak.test.endpoint.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;

import java.util.Optional;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
@DirtiesContext
class RegisterCourseControllerTest {
    private static final String COURSE_REGISTER = "school::education::courses:register";
    private static final String COURSE_UN_REGISTER = "school::education::courses:unregister";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/register/";
    @Mock
    Student student;
    @Mock
    Course course;
    @Captor
    ArgumentCaptor<Student> studentCapture;
    @Captor
    ArgumentCaptor<Course> courseCapture;

    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    CoursesFacade coursesFacade;
    @MockitoSpyBean
    @Autowired
    RegisterCourseController controller;
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
    void shouldRegisterCourse() throws Exception {
        Long studentId = 100L;
        Long courseId = 200L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.put(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);
        checkControllerAspect();
    }

    @Test
    void shouldNotRegisterCourse_NoRoomInTheCourseException() throws Exception {
        Long studentId = 102L;
        Long courseId = 202L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String errorMessage = "No free rooms for the student";
        var exception = new CourseHasNoRoomException(errorMessage);
        doThrow(exception).when(persistenceFacade).link(student, course);
        String requestPath = ROOT + studentId + "/to/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);

        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
        checkControllerAspect();
    }

    @Test
    void shouldNotRegisterCourse_StudentCoursesExceedException() throws Exception {
        Long studentId = 103L;
        Long courseId = 203L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String errorMessage = "Too many courses for the student";
        var exception = new StudentCoursesExceedException(errorMessage);
        doThrow(exception).when(persistenceFacade).link(student, course);

        String requestPath = ROOT + studentId + "/to/" + courseId;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);

        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
        checkControllerAspect();
    }

    @Test
    void shouldUnRegisterCourse() throws Exception {
        Long studentId = 101L;
        Long courseId = 201L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).unRegisterCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_UN_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(RegisterCourseController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(RegisterCourseController.class);
    }
}

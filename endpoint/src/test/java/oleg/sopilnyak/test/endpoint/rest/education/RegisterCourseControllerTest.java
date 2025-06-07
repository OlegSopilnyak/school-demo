package oleg.sopilnyak.test.endpoint.rest.education;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class RegisterCourseControllerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    CoursesFacade coursesFacade;
    @Mock
    StudentsFacade studentsFacade;
    @Spy
    @InjectMocks
    RegisterCourseController controller;
    @Mock
    Student student;
    @Mock
    Course course;

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
        when(studentsFacade.findById(studentId)).thenReturn(Optional.of(student));
        when(coursesFacade.findById(courseId)).thenReturn(Optional.of(course));
        String requestPath = "/register/" + studentId + "/to/" + courseId;
        mockMvc.perform(
                        MockMvcRequestBuilders.put(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).register(student, course);
    }

    @Test
    void shouldNotRegisterCourse_NoRoomInTheCourseException() throws Exception {
        Long studentId = 102L;
        Long courseId = 202L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        when(studentsFacade.findById(studentId)).thenReturn(Optional.of(student));
        when(coursesFacade.findById(courseId)).thenReturn(Optional.of(course));
        String errorMessage = "No room for student";
        doThrow(new CourseHasNoRoomException(errorMessage)).when(coursesFacade).register(student, course);
        String requestPath = "/register/" + studentId + "/to/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void shouldNotRegisterCourse_StudentCoursesExceedException() throws Exception {
        Long studentId = 103L;
        Long courseId = 203L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        when(studentsFacade.findById(studentId)).thenReturn(Optional.of(student));
        when(coursesFacade.findById(courseId)).thenReturn(Optional.of(course));
        String requestPath = "/register/" + studentId + "/to/" + courseId;
        String errorMessage = "Too many courses for student";
        doThrow(new StudentCoursesExceedException(errorMessage)).when(coursesFacade).register(student, course);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void shouldUnRegisterCourse() throws Exception {
        Long studentId = 101L;
        Long courseId = 201L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        when(studentsFacade.findById(studentId)).thenReturn(Optional.of(student));
        when(coursesFacade.findById(courseId)).thenReturn(Optional.of(course));
        String requestPath = "/register/" + studentId + "/to/" + courseId;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).unRegisterCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).unRegister(student, course);
    }
}
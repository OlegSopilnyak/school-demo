package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.exception.NoRoomInTheCourseException;
import oleg.sopilnyak.test.school.common.exception.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class RegisterCourseControllerTest {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    CoursesFacade facade;
    @Spy
    @InjectMocks
    RegisterCourseController controller;

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
        String requestPath = "/register/" + studentId + "/to/" + courseId;
        mockMvc.perform(
                        MockMvcRequestBuilders.put(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).registerCourse(studentId.toString(), courseId.toString());
        verify(facade).register(studentId, courseId);
    }

    @Test
    void shouldNotRegisterCourse_NoRoomInTheCourseException() throws Exception {
        Long studentId = 102L;
        Long courseId = 202L;
        String requestPath = "/register/" + studentId + "/to/" + courseId;
        doThrow(new NoRoomInTheCourseException("No room for student")).when(facade).register(studentId, courseId);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(409).isEqualTo(error.getErrorCode());
        assertThat("No room for student: 102 in course: 202").isEqualTo(error.getErrorMessage());
    }

    @Test
    void shouldNotRegisterCourse_StudentCoursesExceedException() throws Exception {
        Long studentId = 103L;
        Long courseId = 203L;
        String requestPath = "/register/" + studentId + "/to/" + courseId;
        doThrow(new StudentCoursesExceedException("Too many courses for student")).when(facade).register(studentId, courseId);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(409).isEqualTo(error.getErrorCode());
        assertThat("Too many courses for student:103").isEqualTo(error.getErrorMessage());
    }

    @Test
    void shouldUnRegisterCourse() throws Exception {
        Long studentId = 101L;
        Long courseId = 201L;
        String requestPath = "/register/" + studentId + "/to/" + courseId;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).unRegisterCourse(studentId.toString(), courseId.toString());
        verify(facade).unRegister(studentId, courseId);
    }
}
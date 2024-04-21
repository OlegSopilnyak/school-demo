package oleg.sopilnyak.test.endpoint.end2end.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.rest.RegisterCourseController;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.school.common.business.CoursesFacade;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {BusinessLogicConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class RegisterCourseControllerTest extends MysqlTestModelFactory {
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static String ROOT = RequestMappingRoot.REGISTER;

    @Autowired
    PersistenceFacade database;
    @Autowired
    CoursesFacade facade;
    RegisterCourseController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = spy(new RegisterCourseController(facade));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegisterCourse() throws Exception {
        Student student = getPersistent(makeClearStudent(0));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        String requestPath = ROOT + "/" + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.put(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).registerCourse(studentId.toString(), courseId.toString());

        assertThat(course.getStudents()).contains(student);
        assertThat(student.getCourses()).contains(course);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegisterCourse_NoRoomInTheCourseException() throws Exception {
        Student student = getPersistent(makeClearStudent(0));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        IntStream.range(0, 100).forEach(i -> {
            if (course instanceof CourseEntity ce) {
                ce.add(getPersistent(makeClearStudent(i + 1)));
            }
        });
        String requestPath = ROOT + "/" + studentId + "/to/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error =
                MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("No room for student: " + studentId + " in course: " + courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegisterCourse_StudentCoursesExceedException() throws Exception {
        Student student = getPersistent(makeClearStudent(0));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        IntStream.range(0, 100).forEach(i -> {
            if (student instanceof StudentEntity se) {
                se.add(getPersistent(makeClearCourse(i + 1)));
            }
        });
        String requestPath = ROOT + "/" + studentId + "/to/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error =
                MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Too many courses for student:" + studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnRegisterCourse() throws Exception {
        Student student = getPersistent(makeClearStudent(0));
        Course course = getPersistent(makeClearCourse(0));
        if (student instanceof StudentEntity se) {
            se.add(course);
        }
        Long studentId = student.getId();
        Long courseId = course.getId();
        String requestPath = ROOT + "/" + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).unRegisterCourse(studentId.toString(), courseId.toString());

        assertThat(database.findCourseById(courseId).orElseThrow().getStudents()).isEmpty();
        assertThat(database.findStudentById(studentId).orElseThrow().getCourses()).isEmpty();
    }

    private Course getPersistent(Course newInstance) {
        Optional<Course> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private Student getPersistent(Student newInstance) {
        Optional<Student> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
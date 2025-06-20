package oleg.sopilnyak.test.endpoint.end2end.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.stream.IntStream;
import oleg.sopilnyak.test.endpoint.aspect.AspectDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.rest.education.RegisterCourseController;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AspectForRestConfiguration.class, BusinessLogicConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class RegisterCourseControllerTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/register/";

    @Autowired
    PersistenceFacade database;
    @Autowired
    CoursesFacade coursesFacade;
    @Autowired
    StudentsFacade studentsFacade;
    @Autowired
    CommandsFactory<CourseCommand<?>> courseFactory;
    @Autowired
    CommandsFactory<StudentCommand<?>> studentFactory;
    @SpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @SpyBean
    @Autowired
    RegisterCourseController controller;
    @SpyBean
    @Autowired
    AspectDelegate delegate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    @Transactional
    void everythingShouldBeValid() {
        assertThat(courseFactory).isNotNull();
        assertThat(studentFactory).isNotNull();
        assertThat(mapper).isNotNull();
        assertThat(database).isNotNull();

        assertThat(coursesFacade).isNotNull();
        assertThat(courseFactory).isEqualTo(ReflectionTestUtils.getField(coursesFacade, "factory"));
        assertThat(mapper).isEqualTo(ReflectionTestUtils.getField(coursesFacade, "mapper"));

        assertThat(studentsFacade).isNotNull();
        assertThat(studentFactory).isEqualTo(ReflectionTestUtils.getField(studentsFacade, "factory"));
        assertThat(mapper).isEqualTo(ReflectionTestUtils.getField(studentsFacade, "mapper"));

        assertThat(controller).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(coursesFacade).isEqualTo(ReflectionTestUtils.getField(controller, "coursesFacade"));
        assertThat(studentsFacade).isEqualTo(ReflectionTestUtils.getField(controller, "studentsFacade"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegisterCourse() throws Exception {
        Student student = getPersistent(makeClearStudent(0));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.put(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());

        assertThat(course.getStudents()).contains(student);
        assertThat(student.getCourses()).contains(course);
        checkControllerAspect();
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
        String requestPath = ROOT + studentId + "/to/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        var error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Course with ID:" + courseId + " does not have enough rooms.");
        checkControllerAspect();
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
        String requestPath = ROOT + studentId + "/to/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        var error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Student with ID:" + studentId + " exceeds maximum courses.");
        checkControllerAspect();
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
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).unRegisterCourse(studentId.toString(), courseId.toString());

        assertThat(database.findCourseById(courseId).orElseThrow().getStudents()).isEmpty();
        assertThat(database.findStudentById(studentId).orElseThrow().getCourses()).isEmpty();
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
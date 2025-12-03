package oleg.sopilnyak.test.endpoint.end2end.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.rest.education.RegisterCourseController;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import java.util.Optional;
import java.util.stream.IntStream;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        RegisterCourseController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class RegisterCourseControllerTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/register/";
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    CoursesFacade coursesFacade;
    @Autowired
    StudentsFacade studentsFacade;
    @Autowired
    CommandsFactory<CourseCommand<?>> courseFactory;
    @Autowired
    CommandsFactory<StudentCommand<?>> studentFactory;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
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

    @AfterEach
    void tearDown() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("DELETE FROM StudentEntity");
        int deleted = query.executeUpdate();
        assertThat(deleted).isGreaterThanOrEqualTo(0);
        query = em.createQuery("DELETE FROM CourseEntity");
        deleted = query.executeUpdate();
        assertThat(deleted).isGreaterThanOrEqualTo(0);
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void everythingShouldBeValid() {
        assertThat(courseFactory).isNotNull();
        assertThat(studentFactory).isNotNull();
        assertThat(mapper).isNotNull();

        assertThat(coursesFacade).isNotNull();
        assertThat(courseFactory).isEqualTo(ReflectionTestUtils.getField(coursesFacade, "factory"));

        assertThat(studentsFacade).isNotNull();
        assertThat(studentFactory).isEqualTo(ReflectionTestUtils.getField(studentsFacade, "factory"));

        assertThat(controller).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(coursesFacade).isEqualTo(ReflectionTestUtils.getField(controller, "coursesFacade"));
        assertThat(studentsFacade).isEqualTo(ReflectionTestUtils.getField(controller, "studentsFacade"));
    }

    @Test
    void shouldRegisterCourse() throws Exception {
        Student student = getPersistent(makeClearStudent(1));
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

        assertThat(findCourseById(courseId).orElseThrow().getStudents()).contains(student);
        assertThat(findStudentById(studentId).orElseThrow().getCourses()).contains(course);
        checkControllerAspect();
    }

    @Test
    void shouldNotRegisterCourse_NoRoomInTheCourseException() throws Exception {
        Student student = getPersistent(makeClearStudent(2));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        IntStream.range(0, 100).forEach(i -> {
            if (course instanceof CourseEntity ce) {
                ce.add(makeClearStudent(i + 10));
            }
        });

        if (course instanceof CourseEntity ce) {
            ce.setId(null);
            courseId = getPersistent(ce).getId();
        }

        assertThat(findCourseById(courseId)).isPresent();
        assertThat(findCourseById(courseId).orElseThrow().getStudents()).isNotEmpty();
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
    void shouldNotRegisterCourse_StudentCoursesExceedException() throws Exception {
        Student student = getPersistent(makeClearStudent(3));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        IntStream.range(0, 100).forEach(i -> {
            if (student instanceof StudentEntity se) {
                se.add(makeClearCourse(i + 10));
            }
        });
        if (student instanceof StudentEntity se) {
            se.setId(null);
            se.setProfileId(13L);
            studentId = getPersistent(se).getId();
        }
        assertThat(findStudentById(studentId)).isPresent();
        assertThat(findStudentById(studentId).orElseThrow().getCourses()).isNotEmpty();

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
    void shouldUnRegisterCourse() throws Exception {
        Student student = getPersistent(makeClearStudent(4));
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

        assertThat(findCourseById(courseId).orElseThrow().getStudents()).isEmpty();
        assertThat(findStudentById(studentId).orElseThrow().getCourses()).isEmpty();
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

    private Student getPersistent(Student newInstance) {
        StudentEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getCourses().forEach(em::persist);
            em.getTransaction().commit();
            return entity;
        }
    }

    private Optional<Student> findStudentById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            StudentEntity entity = em.find(StudentEntity.class, id);
            if (entity != null) {
                entity.getCourseSet().forEach(course -> course.getStudentSet().size());
            }
            return Optional.ofNullable(entity);
        }
    }

    private Course getPersistent(Course newInstance) {
        CourseEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getStudents().forEach(em::persist);
            em.getTransaction().commit();
            return entity;
        }
    }

    private Optional<Course> findCourseById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            CourseEntity entity = em.find(CourseEntity.class, id);
            if (entity != null) {
                entity.getStudents().forEach(student -> student.getCourses().size());
            }
            return Optional.ofNullable(entity);
        }
    }
}
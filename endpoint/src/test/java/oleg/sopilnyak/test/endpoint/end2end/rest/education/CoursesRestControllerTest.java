package oleg.sopilnyak.test.endpoint.end2end.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.rest.education.CoursesRestController;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import java.util.Comparator;
import java.util.List;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        CoursesRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class CoursesRestControllerTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/courses";

    @Autowired
    PersistenceFacade database;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    CommandsFactory<CourseCommand<?>> factory;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
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
        assertThat(factory).isNotNull();
        assertThat(mapper).isNotNull();
        assertThat(database).isNotNull();

        assertThat(facade).isNotNull();
        assertThat(factory).isEqualTo(ReflectionTestUtils.getField(facade, "factory"));

        assertThat(controller).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(facade).isEqualTo(ReflectionTestUtils.getField(controller, "facade"));
    }

    @Test
    void shouldFindCourse() throws Exception {
        Course course = getPersistent(makeClearTestCourse());
        Long id = course.getId();
        String requestPath = ROOT + "/" + id;

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

        assertCourseEquals(course, courseDto);
        checkControllerAspect();
    }

    @Test
    void shouldFindEnrolledFor() throws Exception {
        Student student = getPersistent(makeClearStudent(0));
        Long studentId = student.getId();
        int coursesAmount = 10;
        IntStream.range(1, coursesAmount + 1).forEach(i -> {
            if (student instanceof StudentEntity se) se.add(makeClearCourse(i));
        });
        persist(student);
        String requestPath = ROOT + "/registered/" + studentId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findRegisteredFor(studentId.toString());
        List<Course> courses = findStudentById(studentId).orElseThrow().getCourses();
        String responseString = result.getResponse().getContentAsString();
        var courseList = MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
        }).stream().map(Course.class::cast).toList();

        assertThat(courseList).hasSize(coursesAmount);
        assertCourseLists(courses, courseList);
        checkControllerAspect();
    }

    @Test
    void shouldFindEmptyCourses() throws Exception {
        int coursesAmount = 5;
        IntStream.range(0, coursesAmount).forEach(i -> getPersistent(makeClearCourse(i + 1)));
        String requestPath = ROOT + "/empty";

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
        var courseList = MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
        }).stream().map(Course.class::cast).toList();

        assertThat(courseList).hasSize(coursesAmount);
        List<Course> courses = database.findCoursesWithoutStudents().stream()
                .sorted(Comparator.comparing(Course::getName)).toList();
        assertCourseLists(courses, courseList);
        checkControllerAspect();
    }

    @Test
    void shouldCreateCourse() throws Exception {
        Course course = makeClearCourse(0);
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).createCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertCourseEquals(course, courseDto, false);
        checkControllerAspect();
    }

    @Test
    void shouldUpdateValidCourse() throws Exception {
        Course course = getPersistent(makeClearCourse(1));
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertCourseEquals(course, courseDto, true);
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateInvalidCourse_NullId() throws Exception {
        Course course = makeTestCourse(null);
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: 'null'");
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateInvalidCourse_NegativeId() throws Exception {
        Long id = -101L;
        Course course = makeTestCourse(id);
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '-101'");
        checkControllerAspect();
    }

    @Test
    void shouldDeleteCourseValidId() throws Exception {
        Long id = getPersistent(makeClearCourse(1)).getId();
        String requestPath = ROOT + "/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteCourse(id.toString());
        assertThat(findCourseById(id)).isEmpty();
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteCourse_CourseNotExistsException() throws Exception {
        long id = 103L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(String.valueOf(id));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '103'");
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteCourse_CourseWithStudentsException() throws Exception {
        Course course = getPersistent(makeClearTestCourse());
        Long id = course.getId();
        assertThat(findCourseById(id)).isPresent();
        assertThat(findCourseById(id).orElseThrow().getStudents()).isNotEmpty();
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(id.toString());
        assertThat(findCourseById(id)).isPresent();
        ActionErrorMessage error =
                MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Course with ID:" + id + " has enrolled students.");
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

    private void persist(Student instance) {
        StudentEntity entity = instance instanceof StudentEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private Student getPersistent(Student newInstance) {
        StudentEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
        }
    }

    private Optional<Student> findStudentById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            StudentEntity entity = em.find(StudentEntity.class, id);
            if (entity != null) {
                entity.getCourseSet().forEach(course -> course.getStudents().size());
            }
            return Optional.ofNullable(entity);
        } finally {
            em.close();
        }
    }

    private Course getPersistent(Course newInstance) {
        CourseEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getStudents().forEach(em::persist);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
        }
    }

    private Optional<Course> findCourseById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            CourseEntity entity = em.find(CourseEntity.class, id);
            if (entity != null) {
                entity.getStudents().forEach(student -> student.getCourses().size());
            }
            return Optional.ofNullable(entity);
        } finally {
            em.close();
        }
    }
}
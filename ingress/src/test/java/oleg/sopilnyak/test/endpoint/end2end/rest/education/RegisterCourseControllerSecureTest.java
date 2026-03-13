package oleg.sopilnyak.test.endpoint.end2end.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
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
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
class RegisterCourseControllerSecureTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/register/";
    @Autowired
    EntityMapper entityMapper;
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
    @Autowired
    FilterChainProxy springSecurityFilterChain;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .apply(springSecurity(springSecurityFilterChain))
                .build();
    }

    @AfterEach
    void tearDown() {
        deleteEntities(StudentEntity.class);
        deleteEntities(CourseEntity.class);
        deleteEntities(FacultyEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
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
        assertThat(authenticationFacade).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(coursesFacade).isEqualTo(ReflectionTestUtils.getField(controller, "coursesFacade"));
        assertThat(studentsFacade).isEqualTo(ReflectionTestUtils.getField(controller, "studentsFacade"));
    }

    @Test
    void shouldRegisterCourse() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(1, Permission.EDU_UPDATE);
        // prepare the test
        Student student = getPersistent(makeClearStudent(1));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.put(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());

        assertThat(findCourseById(courseId).orElseThrow().getStudents()).contains(student);
        assertThat(findStudentById(studentId).orElseThrow().getCourses()).contains(course);
        checkControllerAspect();
    }

    @Test
    void shouldNotRegisterCourse_WrongPermission() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(2, Permission.EDU_GET);
        // prepare the test
        Student student = getPersistent(makeClearStudent(1));
        Course course = getPersistent(makeClearCourse(0));
        Long studentId = student.getId();
        Long courseId = course.getId();
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.put(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isUnauthorized())
                .andDo(print());

        verify(controller, never()).registerToCourse(anyString(), anyString());
    }

    @Test
    void shouldNotRegisterCourse_NoRoomInTheCourseException() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(3, Permission.EDU_UPDATE);
        // prepare the test
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
                                        .header("Authorization", "Bearer " + credentials.getToken())
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
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(4, Permission.EDU_UPDATE);
        // prepare the test
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
                                        .header("Authorization", "Bearer " + credentials.getToken())
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
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(5, Permission.EDU_UPDATE);
        // prepare the test
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
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).unRegisterCourse(studentId.toString(), courseId.toString());

        assertThat(findCourseById(courseId).orElseThrow().getStudents()).isEmpty();
        assertThat(findStudentById(studentId).orElseThrow().getCourses()).isEmpty();
        checkControllerAspect();
    }

    @Test
    void shouldNotUnRegisterCourse_WrongPermission() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(6, Permission.EDU_LIST);
        // prepare the test
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
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isUnauthorized())
                .andDo(print());

        verify(controller, never()).unRegisterCourse(anyString(), anyString());
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(RegisterCourseController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(RegisterCourseController.class);
    }

    private AccessCredentials signInWith(int order, Permission permission) {
        String username = UUID.randomUUID().toString();
        String password = "password";
        // prepare dataset
        AuthorityPerson person = createAuthorityPerson(order, username, password, permission);
        assertThat(person).isNotNull();
        // signing in the person
        Optional<AccessCredentials> credentials = authenticationFacade.signIn(username, password);
        assertThat(credentials).isPresent();
        return credentials.orElseThrow();
    }


    private Student getPersistent(Student newInstance) {
        StudentEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getCourses().forEach(em::persist);
            em.getTransaction().commit();
            return entity;
        }
    }

    private Optional<Student> findStudentById(Long id) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            StudentEntity entity = em.find(StudentEntity.class, id);
            if (entity != null) {
                entity.getCourseSet().forEach(course -> course.getStudentSet().size());
            }
            return Optional.ofNullable(entity);
        }
    }

    private Course getPersistent(Course newInstance) {
        CourseEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getStudents().forEach(em::persist);
            em.getTransaction().commit();
            return entity;
        }
    }

    private Optional<Course> findCourseById(Long id) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            CourseEntity entity = em.find(CourseEntity.class, id);
            if (entity != null) {
                entity.getStudents().forEach(student -> student.getCourses().size());
            }
            return Optional.ofNullable(entity);
        }
    }
    private AuthorityPerson createAuthorityPerson(int id, String username, String password, Permission permission) {
        AuthorityPerson person = create(makeCleanAuthorityPerson(id));
        setPersonPermissions(person, username, password, permission);
        assertThat(database.updateAccess(person, username, password)).isTrue();
        if (person instanceof AuthorityPersonEntity entity) {
            entity.setFaculties(List.of());
            merge(entity);
        }
        return person;
    }

    private void merge(AuthorityPerson instance) {
        AuthorityPersonEntity entity = instance instanceof AuthorityPersonEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }
    }

    private PrincipalProfile persist(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private AuthorityPerson create(AuthorityPerson person) {
        PrincipalProfile profile = persist(makePrincipalProfile(null));
        if (person instanceof FakeAuthorityPerson fake) {
            fake.setProfileId(profile.getId());
        } else {
            fail("Invalid person type '{}'", person.getClass());
        }
        return persist(person);
    }

    private AuthorityPerson persist(AuthorityPerson newInstance) {
        AuthorityPersonEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private void setPersonPermissions(AuthorityPerson person, String username, String password, Permission permission) {
        PrincipalProfileEntity profile = findEntity(PrincipalProfileEntity.class, person.getProfileId());
        profile.setUsername(username);
        profile.setPermissions(Set.of(permission));
        try {
            profile.setSignature(profile.makeSignatureFor(password));
            merge(profile);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
        }
    }

    private void merge(PrincipalProfile instance) {
        PrincipalProfileEntity entity = instance instanceof PrincipalProfileEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }
    }
}

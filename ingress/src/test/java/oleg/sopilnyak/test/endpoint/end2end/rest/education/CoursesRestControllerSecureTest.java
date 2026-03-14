package oleg.sopilnyak.test.endpoint.end2end.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
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
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.util.ObjectUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        CoursesRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
class CoursesRestControllerSecureTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/courses";

    @Autowired
    PersistenceFacade database;
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
        assertThat(factory).isNotNull();
        assertThat(mapper).isNotNull();
        assertThat(database).isNotNull();

        assertThat(facade).isNotNull();
        assertThat(factory).isEqualTo(ReflectionTestUtils.getField(facade, "factory"));

        assertThat(controller).isNotNull();
        assertThat(springSecurityFilterChain).isNotNull();
        assertThat(authenticationFacade).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(facade).isEqualTo(ReflectionTestUtils.getField(controller, "facade"));
    }

    @Test
    void shouldFindCourse() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(1, List.of(Permission.EDU_GET));
        // prepare the test
        Course course = getPersistent(makeClearTestCourse());
        Long id = course.getId();
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);
        assertCourseEquals(course, courseDto);
        // check the behavior
        verify(controller).findCourse(id.toString());
        checkControllerAspect();
    }

    @Test
    void shouldFindEnrolledFor() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(2, List.of(Permission.EDU_LIST, Permission.EDU_GET));
        // prepare the test
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
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        var courseList = MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
        }).stream().map(Course.class::cast).toList();
        assertThat(courseList).hasSize(coursesAmount);
        assertCourseLists(findStudentById(studentId).orElseThrow().getCourses(), courseList);
        // check the behavior
        verify(controller).findRegisteredFor(studentId.toString());
        checkControllerAspect();
    }

    @Test
    void shouldFindEmptyCourses() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(3, List.of(Permission.EDU_LIST, Permission.EDU_GET));
        // prepare the test
        int coursesAmount = 5;
        IntStream.range(0, coursesAmount).forEach(i -> getPersistent(makeClearCourse(i + 1)));
        String requestPath = ROOT + "/empty";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        var courseList = MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
        }).stream().map(Course.class::cast).toList();
        assertThat(courseList).hasSize(coursesAmount);
        List<Course> stored = findCoursesWithoutStudents().stream().sorted(Comparator.comparing(Course::getName)).toList();
        assertCourseLists(stored, courseList);
        // check the behavior
        verify(controller).findEmptyCourses();
        checkControllerAspect();
    }

    @Test
    void shouldCreateCourse() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(4, List.of(Permission.EDU_CREATE, Permission.EDU_GET));
        // prepare the test
        Course course = makeClearCourse(0);
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);
        assertCourseEquals(course, courseDto, false);
        // check the behavior
        verify(controller).createCourse(any(CourseDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldUpdateValidCourse() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(5, List.of(Permission.EDU_UPDATE, Permission.EDU_GET));
        // prepare the test
        Course course = getPersistent(makeClearCourse(1));
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);
        assertCourseEquals(course, courseDto, true);
        // check the behavior
        verify(controller).updateCourse(any(CourseDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateInvalidCourse_NullId() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(6, List.of(Permission.EDU_UPDATE, Permission.EDU_GET));
        // prepare the test
        Course course = makeTestCourse(null);
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: 'null'");
        // check the behavior
        verify(controller).updateCourse(any(CourseDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateInvalidCourse_NegativeId() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(7, List.of(Permission.EDU_UPDATE, Permission.EDU_GET));
        // prepare the test
        Long id = -101L;
        Course course = makeTestCourse(id);
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '-101'");
        // check the behavior
        verify(controller).updateCourse(any(CourseDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldDeleteCourseValidId() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(8, List.of(Permission.EDU_DELETE, Permission.EDU_GET));
        // prepare the test
        Long id = getPersistent(makeClearCourse(1)).getId();
        String requestPath = ROOT + "/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        // check the results
        assertThat(findCourseById(id)).isEmpty();
        // check the behavior
        verify(controller).deleteCourse(id.toString());
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteCourse_CourseNotExistsException() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(9, List.of(Permission.EDU_DELETE));
        // prepare the test
        long id = 103L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '103'");
        // check the behavior
        verify(controller).deleteCourse(String.valueOf(id));
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteCourse_CourseWithStudentsException() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(10, List.of(Permission.EDU_DELETE));
        // prepare the test
        Course course = getPersistent(makeClearTestCourse());
        Long id = course.getId();
        Course entity = findCourseById(id).orElseGet(() -> {
            fail("Not found course");
            return null;
        });
        assertThat(entity.getStudents()).isNotEmpty();
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        // check the results
        assertThat(findCourseById(id)).isPresent();
        ActionErrorMessage error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).isEqualTo("Course with ID:" + id + " has enrolled students.");
        // check the behavior
        verify(controller).deleteCourse(id.toString());
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

    private List<Course> findCoursesWithoutStudents() {
        Predicate<CourseEntity> restriction = entity -> ObjectUtils.isEmpty(entity.getStudents());
        Consumer<CourseEntity> forEachEntity = entity -> entity.getStudents().size();
        return findFor(CourseEntity.class, restriction, forEachEntity).stream().map(Course.class::cast).toList();
    }

    private AccessCredentials signInWith(int order, List<Permission> permissions) {
        String username = UUID.randomUUID().toString();
        String password = "password";
        // prepare dataset
        AuthorityPerson person = createAuthorityPerson(order, username, password, permissions);
        assertThat(person).isNotNull();
        // signing in the person
        Optional<AccessCredentials> credentials = authenticationFacade.signIn(username, password);
        assertThat(credentials).isPresent();
        return credentials.orElseThrow();
    }

    private AuthorityPerson createAuthorityPerson(int id, String username, String password, List<Permission> permissions) {
        AuthorityPerson person = create(makeCleanAuthorityPerson(id));
        setPersonPermissions(person, username, password, permissions);
        assertThat(database.updateAccess(person, username, password)).isTrue();
        if (person instanceof AuthorityPersonEntity entity) {
            entity.setFaculties(List.of());
            merge(entity);
        }
        return person;
    }

    private void setPersonPermissions(AuthorityPerson person, String username, String password, List<Permission> permissions) {
        PrincipalProfileEntity profile = findEntity(PrincipalProfileEntity.class, person.getProfileId());
        profile.setUsername(username);
        profile.setPermissions(Set.copyOf(permissions));
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

    private void persist(Student instance) {
        StudentEntity entity = instance instanceof StudentEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }
    }

    private Student getPersistent(Student newInstance) {
        StudentEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
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
        return Optional.ofNullable(findEntity(CourseEntity.class, id, entity -> entity.getStudents().size()));
    }

    private Optional<Student> findStudentById(Long id) {
        return Optional.ofNullable(findEntity(StudentEntity.class, id, this::refreshStudentCourses));
    }

    private void refreshStudentCourses(StudentEntity entity) {
        entity.getCourses().forEach(student -> student.getStudents().size());
    }
}
package oleg.sopilnyak.test.endpoint.end2end.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.rest.education.StudentsRestController;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import jakarta.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.aspectj.lang.JoinPoint;
import org.assertj.core.api.Assertions;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        StudentsRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
class StudentsRestControllerSecureTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/students";

    @Autowired
    PersistenceFacade database;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    CommandsFactory<StudentCommand<?>> factory;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @Autowired
    StudentsFacade facade;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    StudentsRestController controller;
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
        deleteEntities(StudentProfileEntity.class);
        deleteEntities(StudentEntity.class);
        deleteEntities(CourseEntity.class);
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
    void shouldFindStudent() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(1, List.of(Permission.EDU_GET));
        // prepare the test
        Student student = getPersistent(makeClearTestStudent());
        Long id = student.getId();
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
        StudentDto studentDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentDto.class);
        assertStudentEquals(student, studentDto);
        // check the behavior
        verify(controller).findStudent(id.toString());
        checkControllerAspect();
    }

    @Test
    void shouldFindStudentsEnrolledForCourse() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(2, List.of(Permission.EDU_GET, Permission.EDU_LIST));
        // prepare the test
        Course course = persist(makeClearTestCourse());
        Long courseId = course.getId();
        int studentsAmount = course.getStudents().size();

        String requestPath = ROOT + "/enrolled/" + courseId;

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
        List<Student> studentList = MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
        }).stream().map(Student.class::cast).toList();
        assertThat(studentList).hasSize(studentsAmount);
        assertStudentLists(findCourseById(courseId).getStudents(), studentList);
        // check the behavior
        verify(controller).findEnrolledTo(courseId.toString());
        checkControllerAspect();
    }

    @Test
    void shouldFindStudentsWithEmptyCourses() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(3, List.of(Permission.EDU_GET, Permission.EDU_LIST));
        // prepare the test
        int studentsAmount = 5;
        Set<Student> students = IntStream.range(0, studentsAmount)
                .mapToObj(i -> getPersistent(makeClearStudent(i))).collect(Collectors.toSet());
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
        var studentList = MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
        }).stream().map(Student.class::cast).toList();
        assertThat(studentList).hasSize(studentsAmount);
        assertStudentLists(students.stream().sorted(Comparator.comparing(Student::getFullName)).toList(), studentList);
        // check the behavior
        verify(controller).findNotEnrolledStudents();
        checkControllerAspect();
    }

    @Test
    void shouldCreateStudent() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(4, List.of(Permission.EDU_GET, Permission.EDU_CREATE));
        // prepare the test
        Student student = makeClearTestStudent();
        String jsonContent = MAPPER.writeValueAsString(student);

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
        StudentDto studentDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentDto.class);
        assertStudentEquals(student, studentDto, false);
        // check the behavior
        verify(controller).createStudent(any(StudentDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldUpdateValidStudent() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(5, List.of(Permission.EDU_GET, Permission.EDU_UPDATE));
        // prepare the test
        Student student = getPersistent(makeClearStudent(0));
        Long studentId = student.getId();
        String jsonContent = MAPPER.writeValueAsString(student);


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
        StudentDto studentDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentDto.class);
        assertStudentEquals(findStudentById(studentId), studentDto, true);
        // check the behavior
        verify(controller).updateStudent(any(StudentDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldDeleteStudentValidId() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(5, List.of(Permission.EDU_DELETE, Permission.EDU_UPDATE));
        // prepare the test
        Student student = createStudent(makeClearStudent(0));
        Long id = student.getId();
        assertThat(findStudentById(id)).isNotNull();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteStudent(id.toString());
        assertThat(findStudentById(id)).isNull();
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsRestController.class);
    }

    private Student findStudentById(Long id) {
        return findEntity(StudentEntity.class, id, e -> e.getCourseSet().size());
    }

    private Course findCourseById(Long id) {
        return findEntity(CourseEntity.class, id, e -> e.getStudentSet().forEach(student -> student.getCourseSet().size()));
    }

    private Student getPersistent(Student newStudent) {
        return payloadMapper.toPayload(persist(newStudent));
    }

    private StudentPayload createStudent(Student newStudent) {
        try {
            StudentProfile profile = persist(makeStudentProfile(null));
            if (newStudent instanceof FakeStudent fake) {
                fake.setProfileId(profile.getId());
            } else {
                fail("Not a fake person type");
            }
            return payloadMapper.toPayload(persist(newStudent));
        } finally {
            reset(payloadMapper);
        }
    }

    private Student persist(Student newInstance) {
        Student entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private Course persist(Course newInstance) {
        CourseEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            entity.getStudentSet().forEach(em::persist);
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private StudentProfile persist(StudentProfile newInstance) {
        StudentProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
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
            Assertions.fail("Invalid person type '{}'", person.getClass());
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
}

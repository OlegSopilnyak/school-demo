package oleg.sopilnyak.test.endpoint.end2end.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.organization.FacultiesRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
        FacultiesRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
class FacultiesRestControllerSecureTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/faculties";

    @Autowired
    PersistenceFacade database;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    CommandsFactory<FacultyCommand<?>> factory;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @Autowired
    FacultyFacade facade;
    @MockitoSpyBean
    @Autowired
    AdviseDelegate delegate;
    @MockitoSpyBean
    @Autowired
    FacultiesRestController controller;

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
    void shouldFindAllFaculties() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(1, List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        int personsAmount = 10;
        List<Faculty> faculties = makeCleanFaculties(personsAmount).stream().map(this::getPersistent).toList();

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAll();

        var facultyList = MAPPER.<List<FacultyDto>>readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        }).stream().map(Faculty.class::cast).toList();
        assertThat(facultyList).hasSize(personsAmount);
        assertFacultyLists(faculties, facultyList, true);
        checkControllerAspect();
    }

    @Test
    void shouldFindFacultyById() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(2, List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        Faculty faculty = getPersistent(makeCleanFaculty(0));
        Long id = faculty.getId();
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

        verify(controller).findById(id.toString());

        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, dto);
    }

    @Test
    void shouldCreateFaculty() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(3, List.of(Permission.ORG_CREATE, Permission.ORG_GET));
        // prepare the test
        Faculty faculty = makeCleanFaculty(1);
        String jsonContent = MAPPER.writeValueAsString(faculty);

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

        verify(controller).create(any(FacultyDto.class));

        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertThat(dto.getId()).isNotNull();
        assertFacultyEquals(faculty, dto, false);
    }

    @Test
    void shouldUpdateFaculty() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(4, List.of(Permission.ORG_UPDATE, Permission.ORG_GET));
        // prepare the test
        Faculty faculty = getPersistent(makeCleanFaculty(2));
        String jsonContent = MAPPER.writeValueAsString(faculty);

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

        verify(controller).update(any(FacultyDto.class));

        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, dto);
    }

    @Test
    void shouldDeleteFaculty() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(5, List.of(Permission.ORG_DELETE));
        // prepare the test
        Faculty faculty = getPersistent(makeCleanFaculty(2));
        if (faculty instanceof FacultyEntity entity) {
            entity.setCourses(List.of());
            merge(entity);
        }
        Long id = faculty.getId();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteById(id.toString());

        assertThat(findFacultyById(id)).isNull();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(FacultiesRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(FacultiesRestController.class);
    }

    private Faculty findFacultyById(Long id) {
        return findEntity(FacultyEntity.class, id, e -> e.getCourseEntitySet().size());
    }

    private Faculty getPersistent(Faculty newInstance) {
        FacultyEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private void merge(Faculty instance) {
        FacultyEntity entity = instance instanceof FacultyEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
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

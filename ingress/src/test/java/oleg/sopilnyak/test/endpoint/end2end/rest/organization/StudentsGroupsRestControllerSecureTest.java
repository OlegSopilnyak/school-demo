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
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.organization.StudentsGroupsRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
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
        StudentsGroupsRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
class StudentsGroupsRestControllerSecureTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/student-groups";

    @Autowired
    PersistenceFacade database;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    CommandsFactory<StudentsGroupCommand<?>> factory;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    StudentsGroupFacade facade;
    @MockitoSpyBean
    @Autowired
    AdviseDelegate delegate;
    @MockitoSpyBean
    @Autowired
    StudentsGroupsRestController controller;

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
        deleteEntities(StudentsGroupEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
    }

    @Test
    void everythingShouldBeValid() {
        assertThat(factory).isNotNull();
        assertThat(payloadMapper).isNotNull();
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
    void shouldFindAllStudentsGroups() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(1, List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        int groupsAmount = 5;
        List<StudentsGroup> studentsGroups = IntStream.range(0, groupsAmount)
                .mapToObj(i -> persist(makeCleanStudentsGroup(i + 1))).toList();

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
        String responseString = result.getResponse().getContentAsString();
        var groupList = MAPPER.readValue(responseString, new TypeReference<List<StudentsGroupDto>>() {
        }).stream().map(StudentsGroup.class::cast).toList();

        assertThat(groupList).hasSize(groupsAmount);
        assertStudentsGroupLists(studentsGroups, groupList);
        checkControllerAspect();
    }

    @Test
    void shouldFindStudentsGroupById() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(2, List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        StudentsGroup studentsGroup = persist(makeCleanStudentsGroup(0));
        Long id = studentsGroup.getId();
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
        String responseString = result.getResponse().getContentAsString();
        var dto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, dto);
        checkControllerAspect();
    }

    @Test
    void shouldCreateStudentsGroup() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(3, List.of(Permission.ORG_CREATE, Permission.ORG_GET));
        // prepare the test
        StudentsGroup studentsGroup = makeCleanStudentsGroup(1);
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

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

        verify(controller).create(any(StudentsGroupDto.class));
        String responseString = result.getResponse().getContentAsString();
        var dto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, dto, false);
        checkControllerAspect();
    }

    @Test
    void shouldUpdateStudentsGroup() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(4, List.of(Permission.ORG_UPDATE, Permission.ORG_GET));
        // prepare the test
        StudentsGroup studentsGroup = persist(makeCleanStudentsGroup(2));
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

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

        verify(controller).update(any(StudentsGroupDto.class));
        String responseString = result.getResponse().getContentAsString();
        var dto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, dto);
        checkControllerAspect();
    }

    @Test
    void shouldDeleteStudentsGroup() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(5, List.of(Permission.ORG_DELETE));
        // prepare the test
        StudentsGroup studentsGroup = persist(makeCleanStudentsGroup(2));
        if (studentsGroup instanceof StudentsGroupEntity) {
            deleteEntities(StudentEntity.class);
        }
        Long id = studentsGroup.getId();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        assertThat(findStudentsGroupById(id)).isNull();
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsGroupsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsGroupsRestController.class);
    }

    private StudentsGroup findStudentsGroupById(Long id) {
        return findEntity(StudentsGroupEntity.class, id, e -> e.getStudentEntitySet().size());
    }

    private StudentsGroup persist(StudentsGroup newInstance) {
        StudentsGroupEntity entity = entityMapper.toEntity(newInstance);
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

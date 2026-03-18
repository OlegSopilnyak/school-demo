package oleg.sopilnyak.test.endpoint.end2end.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.organization.AuthorityPersonsRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
        AuthorityPersonsRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
class AuthorityPersonsRestControllerSecureTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/authorities";
    private static final ThreadLocal<AuthorityPerson> authorityPersonThreadLocal = new ThreadLocal<>();

    @Autowired
    PersistenceFacade database;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    CommandsFactory<AuthorityPersonCommand<?>> factory;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @MockitoSpyBean
    @Autowired
    AuthorityPersonFacade facade;
    @MockitoSpyBean
    @Autowired
    AuthorityPersonsRestController controller;
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
        deleteEntities(FacultyEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
        authorityPersonThreadLocal.remove();
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
    void shouldFindAllAuthorities() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(100, List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        Stream<AuthorityPerson> example = IntStream.range(0, 30).mapToObj(i -> persist(makeCleanAuthorityPerson(i + 1)));
        List<AuthorityPerson> staff = Stream
                // added signed in person to the staff list
                .concat(Stream.of(authorityPersonThreadLocal.get()), example)
                // controller full-name is ordering also
                .sorted(Comparator.comparing(AuthorityPerson::getFullName))
                .toList();
        int staffAmount = staff.size();

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
        var authorityPersonList = MAPPER.readValue(responseString, new TypeReference<List<AuthorityPersonDto>>() {
        }).stream().map(AuthorityPerson.class::cast).toList();
        assertThat(authorityPersonList).hasSize(staffAmount);
        assertAuthorityPersonLists(staff, authorityPersonList);
        checkControllerAspect();
    }

    @Test
    void shouldFindAuthorityPersonById() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(2, List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        AuthorityPerson person = persist(makeCleanAuthorityPerson(100));
        Long id = person.getId();
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
        var personDto = MAPPER.readValue(result.getResponse().getContentAsString(), AuthorityPersonDto.class);
        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldCreateAuthorityPerson() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(3, List.of(Permission.ORG_CREATE, Permission.ORG_GET));
        // prepare the test
        Role role = Role.TEACHER;
        AuthorityPerson person = makeCleanAuthorityPerson(200);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .param("role", role.name())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).createPerson(any(AuthorityPersonDto.class), eq(role));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto, false);
        checkControllerAspect();
    }

    @Test
    void shouldUpdateAuthorityPerson() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(4, List.of(Permission.ORG_UPDATE, Permission.ORG_GET));
        // prepare the test
        AuthorityPerson person = persist(makeCleanAuthorityPerson(201));
        String jsonContent = MAPPER.writeValueAsString(person);

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

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldDeleteAuthorityPerson() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(5, List.of(Permission.ORG_DELETE));
        // prepare the test
        AuthorityPerson person = create(makeCleanAuthorityPerson(202));
        if (person instanceof AuthorityPersonEntity) {
            deleteEntities(FacultyEntity.class);
        }
        Long id = person.getId();
        assertThat(findAuthorityPersonById(id)).isNotNull();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deletePerson(id.toString());
        assertThat(findAuthorityPersonById(id)).isNull();
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthorityPersonsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthorityPersonsRestController.class);
    }

    private AuthorityPerson findAuthorityPersonById(Long id) {
        return findEntity(AuthorityPersonEntity.class, id, e -> e.getFacultyEntitySet().size());
    }

    private AccessCredentials signInWith(int order, List<Permission> permissions) {
        String username = UUID.randomUUID().toString();
        String password = "password";
        // prepare dataset
        AuthorityPerson person = createAuthorityPerson(order, username, password, permissions);
        assertThat(person).isNotNull();
        authorityPersonThreadLocal.set(person);
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
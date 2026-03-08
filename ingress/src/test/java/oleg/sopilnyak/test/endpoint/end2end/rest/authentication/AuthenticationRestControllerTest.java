package oleg.sopilnyak.test.endpoint.end2end.rest.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.rest.authentication.AuthenticationRestController;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AccessCredentialsPayload;

import jakarta.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AuthenticationRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
@WebAppConfiguration
class AuthenticationRestControllerTest extends MysqlTestModelFactory {
    private static final String LOGIN = "school::organization::authority::persons:login";
    private static final String LOGOUT = "school::organization::authority::persons:logout";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/authentication";

    @Autowired
    EntityMapper entityMapper;
    @Autowired
    PersistenceFacade database;
    @MockitoSpyBean
    @Autowired
    AuthorityPersonFacade facade;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @MockitoSpyBean
    @Autowired
    private BusinessMessagePayloadMapper messagePayloadMapper;
    @MockitoSpyBean
    @Autowired
    AuthenticationRestController controller;
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
        deleteEntities(FacultyEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
    }

    @Test
    void checkControllerApplicationContext() {
        assertThat(database).isNotNull();
        assertThat(facade).isNotNull();
        assertThat(authenticationFacade).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(controller).isNotNull();
    }

    @Test
    @WithMockUser(username = "username", roles = {"PRINCIPAL"})
    void shouldLogoutAuthorityPerson() throws Exception {
        String username = "username";
        String password = "password";
        AuthorityPerson person = createAuthorityPerson(1, username, password);
        assertThat(person).isNotNull();
        authenticationFacade.signIn(username, password);
        String requestPath = ROOT + "/logout";

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        // check the behavior
        verify(controller).logout();
        verify(facade).doActionAndResult(LOGOUT, username);
        verify(authenticationFacade).signOut(username);
        checkControllerAspect();
    }

    @Test
    void shouldNotLogoutAuthorityPerson_NotAuthorized() throws Exception {
        String requestPath = ROOT + "/logout";

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized())
                .andDo(print())
                .andReturn();

        // check the behavior
        verify(controller).logout();
        verify(facade, never()).doActionAndResult(anyString(), anyString());
        checkControllerAspect();
    }

    @Test
    void shouldLoginAuthorityPerson() throws Exception {
        String username = "test-username";
        String password = "test-password";
        AuthorityPerson person = createAuthorityPerson(2, username, password);
        assertThat(person).isNotNull();
        String requestPath = ROOT + "/login";

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(requestPath)
                                .param("username", username)
                                .param("password", password)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        AccessCredentials credentials = MAPPER.readValue(responseString, AccessCredentialsPayload.class);
        assertThat(credentials).isNotNull();
        assertThat(credentials.getToken()).isNotBlank();
        assertThat(credentials.getRefreshToken()).isNotBlank();
        // check the behavior
        verify(controller).login(username, password);
        verify(facade).doActionAndResult(LOGIN, username, password);
        verify(authenticationFacade).signIn(username, password);
        checkControllerAspect();
    }

    @Test
    void shouldNotLoginAuthorityPerson_WrongLoginUsername() throws Exception {
        String username = "test-username";
        String password = "test-password";
        String wrongLoginUsername = "wrong-sign-in-username";
        AuthorityPerson person = createAuthorityPerson(3, username, password);
        assertThat(person).isNotNull();
        String requestPath = ROOT + "/login";

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(requestPath)
                                .param("username", wrongLoginUsername)
                                .param("password", password)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andDo(print())
                .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with login:'" + wrongLoginUsername + "', is not found");
        // check the behavior
        verify(controller).login(wrongLoginUsername, password);
        verify(facade).doActionAndResult(LOGIN, wrongLoginUsername, password);
        verify(authenticationFacade).signIn(wrongLoginUsername, password);
        checkControllerAspect();
    }

    @Test
    void shouldNotLoginAuthorityPerson_WrongPassword() throws Exception {
        String username = "test-username";
        String password = "test-password";
        String wrongPassword = "wrong-password";
        AuthorityPerson person = createAuthorityPerson(4, username, password);
        assertThat(person).isNotNull();
        String requestPath = ROOT + "/login";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .param("username", username)
                                        .param("password", wrongPassword)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isForbidden())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(403);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong password for username: " + username);
        // check the behavior
        verify(controller).login(username, wrongPassword);
        verify(facade).doActionAndResult(LOGIN, username, wrongPassword);
        verify(authenticationFacade).signIn(username, wrongPassword);
        checkControllerAspect();
    }

    @Test
    void refresh() {
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate, atLeastOnce()).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthenticationRestController.class);
        verify(delegate, atLeastOnce()).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthenticationRestController.class);
    }

    private AuthorityPerson createAuthorityPerson(int id, String username, String password) {
        AuthorityPerson person = create(makeCleanAuthorityPerson(id));
        setPersonPermissions(person, username, password);
        assertThat(database.updateAccess(person, username, password)).isTrue();
        if (person instanceof AuthorityPersonEntity entity) {
            entity.setFaculties(List.of());
            merge(entity);
        }
        return person;
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

    private void setPersonPermissions(AuthorityPerson person, String username, String password) {
        PrincipalProfileEntity profile = findEntity(PrincipalProfileEntity.class, person.getProfileId());
        profile.setUsername(username);
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

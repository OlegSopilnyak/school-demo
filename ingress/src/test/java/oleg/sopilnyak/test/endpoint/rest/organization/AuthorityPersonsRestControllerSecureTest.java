package oleg.sopilnyak.test.endpoint.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
class AuthorityPersonsRestControllerSecureTest extends TestModelFactory {
    private static final String AUTHORITY_PERSONS_FIND_ALL = "school::organization::authority::persons:find.All";
    private static final String AUTHORITY_PERSONS_FIND_BY_ID = "school::organization::authority::persons:find.By.Id";
    private static final String AUTHORITY_PERSONS_UPDATE = "school::organization::authority::persons:create.Or.Update";
    private static final String AUTHORITY_PERSONS_CREATE_TASK = "school::organization::authority::persons:create.Macro";
    private static final String AUTHORITY_PERSONS_DELETE_TASK = "school::organization::authority::persons:delete.Macro";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/authorities";

    @MockitoBean
    PersistenceFacade persistenceFacade;
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
    AuthorityPersonsRestController controller;
    @MockitoSpyBean
    @Autowired
    AdviseDelegate delegate;

    MockMvc mockMvc;
    @Autowired
    FilterChainProxy springSecurityFilterChain;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .apply(springSecurity(springSecurityFilterChain))
                .build();
    }

    @Test
    void shouldFindAllAuthorities() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        int personsAmount = 10;
        Collection<AuthorityPerson> staff = makeAuthorityPersons(personsAmount);
        doReturn(Set.copyOf(staff)).when(persistenceFacade).findAllAuthorityPersons();

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
        verify(facade).doActionAndResult(AUTHORITY_PERSONS_FIND_ALL);
        String responseString = result.getResponse().getContentAsString();

        List<AuthorityPerson> authorityPersonList =
                MAPPER.readValue(responseString, new TypeReference<List<AuthorityPersonDto>>() {
                }).stream().map(AuthorityPerson.class::cast).toList();

        assertThat(authorityPersonList).hasSize(personsAmount);
        assertAuthorityPersonLists(staff.stream().toList(), authorityPersonList);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindAllAuthorities_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isUnauthorized())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(error.getErrorMessage()).isEqualTo("Access Denied");
        // check the behavior
        verify(controller, never()).findAll();
    }

    @Test
    void shouldFindAuthorityPersonById() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        Long id = 300L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonById(id);
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
        verify(facade).doActionAndResult(AUTHORITY_PERSONS_FIND_BY_ID, id);
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindAuthorityPersonById_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 3001L;
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isUnauthorized())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(error.getErrorMessage()).isEqualTo("Access Denied");
        // check the behavior
        verify(controller, never()).findById(anyString());
    }

    @Test
    void shouldCreateAuthorityPerson() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_CREATE, Permission.ORG_GET));
        // prepare the test
        Role role = Role.HEAD_TEACHER;
        AuthorityPerson person = makeTestAuthorityPerson(null);
        String commandId = AUTHORITY_PERSONS_CREATE_TASK;
        doAnswer(invocation -> {
            AuthorityPerson received = invocation.getArgument(1);
            assertThat(received.getId()).isNull();
            assertAuthorityPersonEquals(person, received);
            return Optional.of(person);
        }).when(facade).doActionAndResult(eq(commandId), any(AuthorityPerson.class), eq(role));
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
        verify(facade).doActionAndResult(eq(commandId), any(AuthorityPerson.class), eq(role));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotCreateAuthorityPerson_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        Role role = Role.HEAD_TEACHER;
        AuthorityPerson person = makeTestAuthorityPerson(null);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .param("role", role.name())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isUnauthorized())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(error.getErrorMessage()).isEqualTo("Access Denied");
        // check the behavior
        verify(controller, never()).createPerson(any(AuthorityPersonDto.class), any(Role.class));
    }

    @Test
    void shouldUpdateAuthorityPerson() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_UPDATE, Permission.ORG_GET));
        // prepare the test
        Long id = 301L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        String commandId = AUTHORITY_PERSONS_UPDATE;
        doAnswer(invocation -> {
            AuthorityPerson received = invocation.getArgument(1);
            assertThat(received.getId()).isEqualTo(id);
            assertAuthorityPersonEquals(person, received);
            return Optional.of(person);
        }).when(facade).doActionAndResult(eq(commandId), any(AuthorityPerson.class));
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
        verify(facade).doActionAndResult(eq(commandId), any(AuthorityPerson.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateAuthorityPerson_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 3011L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isUnauthorized())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(error.getErrorMessage()).isEqualTo("Access Denied");
        // check the behavior
        verify(controller, never()).updatePerson(any(AuthorityPersonDto.class));
    }

    @Test
    void shouldDeleteAuthorityPerson() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_DELETE));
        // prepare the test
        Long id = 302L;
        Long profileId = id + 100;
        AuthorityPerson person = spy(makeCleanAuthorityPerson(100));
        doReturn(profileId).when(person).getProfileId();
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonById(id);
        PrincipalProfile profile = messagePayloadMapper.toPayload(makePrincipalProfile(profileId));
        doReturn(Optional.of(profile)).when(persistenceFacade).findPrincipalProfileById(profileId);
        doReturn(profile).when(persistenceFacade).toEntity(profile);
        doReturn(profile).when(messagePayloadMapper).toPayload(profile);
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deletePerson(id.toString());
        verify(facade).doActionAndResult(AUTHORITY_PERSONS_DELETE_TASK, id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteAuthorityPerson_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 3021L;
        String requestPath = ROOT + "/" + id;

        MvcResult result =
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isUnauthorized())
                .andDo(print())
                .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        assertThat(responseString).isNotBlank();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(error.getErrorMessage()).isEqualTo("Access Denied");
        // check the behavior
        verify(controller, never()).deletePerson(anyString());
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthorityPersonsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthorityPersonsRestController.class);
    }

    private AccessCredentials signInWith(List<Permission> permissions) throws Exception {
        String username = UUID.randomUUID().toString();
        String password = "password";
        // prepare dataset
        mockingDataSet(username, password, permissions);
        // signing in the person
        Optional<AccessCredentials> credentials = authenticationFacade.signIn(username, password);
        assertThat(credentials).isPresent();
        return credentials.orElseThrow();
    }

    private void mockingDataSet(String username, String password, List<Permission> permissions) throws Exception {
        Long personId = 1L;
        Long profileId = 2L;
        PrincipalProfile profile = makePrincipalProfile(profileId);
        if (profile instanceof TestModelFactory.FakePrincipalProfile fakeProfile) {
            fakeProfile.setUsername(username);
            fakeProfile.setSignature(profile.makeSignatureFor(password));
            fakeProfile.setPermissions(Set.copyOf(permissions));
        } else {
            fail("Invalid type of profile");
        }
        doReturn(Optional.of(profile)).when(persistenceFacade).findPrincipalProfileByLogin(username);
        AuthorityPerson person = makeCleanAuthorityPerson(personId.intValue());
        if (person instanceof TestModelFactory.FakeAuthorityPerson fakeAuthorityPerson) {
            fakeAuthorityPerson.setId(personId);
        } else {
            fail("Invalid type of person");
        }
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonByProfileId(profileId);
    }
}

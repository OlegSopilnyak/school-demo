package oleg.sopilnyak.test.endpoint.rest.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AccessCredentialsPayload;

import java.util.Optional;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
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
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
class AuthenticationRestControllerSecureTest extends TestModelFactory {
    private static final String LOGIN = "school::organization::authority::persons:login";
    private static final String LOGOUT = "school::organization::authority::persons:logout";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/authentication";
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
    AuthenticationRestController controller;
    @MockitoSpyBean
    @Autowired
    AdviseDelegate delegate;
    @Mock
    AccessCredentials accessCredentials;

    MockMvc mockMvc;
    @Autowired
    FilterChainProxy springSecurityFilterChain;

    @BeforeEach
    void setUp() {
        assertThat(springSecurityFilterChain).isNotNull();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .apply(springSecurity(springSecurityFilterChain))
                .build();
    }

    @Test
    void shouldLogoutAuthorityPerson() throws Exception {
        Long profileId = 1L;
        Long personId = 2L;
        String username = "username";
        String password = "password";
        // prepare dataset
        mockingDataSet(personId, profileId, username, password);
        // signing in the person
        Optional<AccessCredentials> credentials = authenticationFacade.signIn(username,password);
        assertThat(credentials).isNotEmpty();
        String activeToken = credentials.get().getToken();
        String requestPath = ROOT + "/logout";

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + activeToken)
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
    @WithMockUser(username = "user-name", roles = {"TEST"})
    void shouldNotLogoutAuthorityPerson_NotSignedIn() throws Exception {
        String requestPath = ROOT + "/logout";
        ArgumentCaptor<String> userNameCaptor = ArgumentCaptor.forClass(String.class);

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andDo(print());

        // check the behavior
        verify(controller).logout();
        verify(facade).doActionAndResult(eq(LOGOUT), userNameCaptor.capture());
        verify(authenticationFacade).signOut(userNameCaptor.getValue());
        checkControllerAspect();
    }

    @Test
    void shouldLoginAuthorityPerson() throws Exception {
        String username = "test-username";
        String password = "test-password";
        String activeToke = "logged.in_person.active.token";
        String refreshToken = "logged.in_person.refresh_token";
        doReturn(activeToke).when(accessCredentials).getToken();
        doReturn(refreshToken).when(accessCredentials).getRefreshToken();
        doReturn(Optional.of(accessCredentials)).when(authenticationFacade).signIn(username, password);
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
        assertThat(credentials.getToken()).isNotBlank().isEqualTo(activeToke);
        assertThat(credentials.getRefreshToken()).isNotBlank().isEqualTo(refreshToken);
        // check the behavior
        verify(controller).login(username, password);
        verify(facade).doActionAndResult(LOGIN, username, password);
        verify(authenticationFacade).signIn(username, password);
        checkControllerAspect();
    }

    @Test
    void shouldNotLoginAuthorityPerson_NoTokens() throws Exception {
        String username = "test-username";
        String password = "test-password";
        String requestPath = ROOT + "/login";

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(requestPath)
                                .param("username", username)
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
        assertThat(error.getErrorMessage()).isEqualTo("Profile with login:'" + username + "', is not found");
        // check the behavior
        verify(controller).login(username, password);
        verify(facade).doActionAndResult(LOGIN, username, password);
        verify(authenticationFacade).signIn(username, password);
        checkControllerAspect();
    }

    @Test
    @WithMockUser(username = "user-name", roles = {"TEST"})
    void shouldRefreshSignedInUserToken() throws Exception {
        String activeToke = "logged.in_person.active.token";
        String refreshToken = "logged.in_person.refresh_token";
        doReturn(activeToke).when(accessCredentials).getToken();
        doReturn(refreshToken).when(accessCredentials).getRefreshToken();
        doReturn(Optional.of(accessCredentials)).when(authenticationFacade).refresh(eq(refreshToken), anyString());
        String requestPath = ROOT + "/refresh";

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(requestPath)
                                .param("token", refreshToken)
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
        assertThat(credentials.getToken()).isEqualTo(activeToke);
        assertThat(credentials.getRefreshToken()).isNotBlank().isEqualTo(refreshToken);
        // check the behavior
        verify(controller).refresh(refreshToken);
        verify(facade, never()).doActionAndResult(anyString(), anyString());
        verify(authenticationFacade).refresh(eq(refreshToken), anyString());
        checkControllerAspect();
    }

    @Test
    void shouldNotRefreshSignedInUserToken_NoTokens() throws Exception {
        String refreshToken = "logged.in_person.refresh_token";
        String requestPath = ROOT + "/refresh";

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(requestPath)
                                .param("token", refreshToken)
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
        verify(controller, never()).refresh(anyString());
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate, atLeastOnce()).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthenticationRestController.class);
        verify(delegate, atLeastOnce()).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(AuthenticationRestController.class);
    }

    private void mockingDataSet(Long personId, Long profileId, String username, String password) throws Exception {
        PrincipalProfile profile = makePrincipalProfile(profileId);
        if(profile instanceof FakePrincipalProfile fakeProfile) {
            fakeProfile.setUsername(username);
            fakeProfile.setSignature(profile.makeSignatureFor(password));
        } else {
            fail("Invalid type of profile");
        }
        doReturn(Optional.of(profile)).when(persistenceFacade).findPrincipalProfileByLogin(username);
        AuthorityPerson person = makeCleanAuthorityPerson(personId.intValue());
        if (person instanceof FakeAuthorityPerson fakeAuthorityPerson) {
            fakeAuthorityPerson.setId(personId);
        } else {
            fail("Invalid type of person");
        }
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonByProfileId(profileId);
    }
}

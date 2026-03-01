package oleg.sopilnyak.test.endpoint.rest.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext
class AuthenticationRestControllerTest extends TestModelFactory {
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @WithMockUser(username = "username", roles = {"PRINCIPAL"})
    @Test
    void shouldLogoutAuthorityPerson() throws Exception {
        String username = "username";
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
    void shouldLoginAuthorityPerson() throws Exception {
        String username = "test-username";
        String password = "test-password";
        String activeToke = "logged.in_person.active.token";
        String refreshToken = "logged.in_person.refresh_token";
        doReturn(activeToke).when(accessCredentials).getToken();
        doReturn(refreshToken).when(accessCredentials).getRefreshToken();
        doReturn(Optional.of(accessCredentials)).when(authenticationFacade).signIn(username, password);
        String requestPath = ROOT + "/login";

        MvcResult result =
                mockMvc.perform(
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
}
package oleg.sopilnyak.test.endpoint.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
@DirtiesContext
class AuthorityPersonsRestControllerTest extends TestModelFactory {
    private static final String LOGIN = "school::organization::authority::person:login";
    private static final String LOGOUT = "school::organization::authority::person:logout";
    private static final String FIND_ALL = "school::organization::authority::person:find.All";
    private static final String FIND_BY_ID = "school::organization::authority::person:find.By.Id";
    private static final String CREATE_NEW = "school::organization::authority::person:create.Macro";
    private static final String CREATE_OR_UPDATE = "school::organization::authority::person:create.Or.Update";
    private static final String DELETE_ALL = "school::organization::authority::person:delete.Macro";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/authorities";
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    AuthorityPersonFacade facade;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    void shouldLogoutAuthorityPerson() throws Exception {
        String token = "logged_in_person_token";
        String bearer = "Bearer " + token;
        String requestPath = ROOT + "/logout";

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", bearer)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).logout(bearer);
        verify(facade).doActionAndResult(LOGOUT, token);
        checkControllerAspect();
    }

    @Test
    void shouldLoginAuthorityPerson() throws Exception {
        Long profileId = 1L;
        String username = "test-username";
        String password = "test-password";
        AuthorityPerson person = makeCleanAuthorityPerson(212);
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonByProfileId(profileId);
        PrincipalProfilePayload profile = mock(PrincipalProfilePayload.class);
        doReturn(true).when(profile).isPassword(password);
        doReturn(profileId).when(profile).getId();
        doReturn(Optional.of(profile)).when(persistenceFacade).findPrincipalProfileByLogin(username);
        doReturn(profile).when(messagePayloadMapper).toPayload(profile);
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

        verify(controller).login(username, password);
        verify(facade).doActionAndResult(LOGIN, username, password);
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);
        assertAuthorityPersonEquals(person, personDto, false);
        checkControllerAspect();
    }

    @Test
    void shouldFindAllAuthorities() throws Exception {
        int personsAmount = 10;
        Collection<AuthorityPerson> staff = makeAuthorityPersons(personsAmount);
        doReturn(Set.copyOf(staff)).when(persistenceFacade).findAllAuthorityPersons();

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(ROOT)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAll();
        verify(facade).doActionAndResult(FIND_ALL);
        String responseString = result.getResponse().getContentAsString();

        List<AuthorityPerson> authorityPersonList =
                MAPPER.readValue(responseString, new TypeReference<List<AuthorityPersonDto>>() {
                }).stream().map(AuthorityPerson.class::cast).toList();

        assertThat(authorityPersonList).hasSize(personsAmount);
        assertAuthorityPersonLists(staff.stream().toList(), authorityPersonList);
        checkControllerAspect();
    }

    @Test
    void shouldFindAuthorityPersonById() throws Exception {
        Long id = 300L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonById(id);
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id.toString());
        verify(facade).doActionAndResult(FIND_BY_ID, id);
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldCreateAuthorityPerson() throws Exception {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        doAnswer(invocation -> {
            AuthorityPerson received = invocation.getArgument(1);
            assertThat(received.getId()).isNull();
            assertAuthorityPersonEquals(person, received);
            return Optional.of(person);
        }).when(facade).doActionAndResult(eq(CREATE_NEW), any(AuthorityPerson.class));
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).createPerson(any(AuthorityPersonDto.class));
        verify(facade).doActionAndResult(eq(CREATE_NEW), any(AuthorityPerson.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldUpdateAuthorityPerson() throws Exception {
        Long id = 301L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        doAnswer(invocation -> {
            AuthorityPerson received = invocation.getArgument(1);
            assertThat(received.getId()).isEqualTo(id);
            assertAuthorityPersonEquals(person, received);
            return Optional.of(person);
        }).when(facade).doActionAndResult(eq(CREATE_OR_UPDATE), any(AuthorityPerson.class));
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        verify(facade).doActionAndResult(eq(CREATE_OR_UPDATE), any(AuthorityPerson.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateAuthorityPerson_WrongId_Negative() throws Exception {
        Long id = -301L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '-301'");
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateAuthorityPerson_WrongId_Null() throws Exception {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: 'null'");
        checkControllerAspect();
    }

    @Test
    void shouldDeleteAuthorityPerson() throws Exception {
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
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deletePerson(id.toString());
        verify(facade).doActionAndResult(DELETE_ALL, id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteAuthorityPerson_WrongId_Null() throws Exception {
        String requestPath = ROOT + "/" + null;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson("null");
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: 'null'");
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteAuthorityPerson_WrongId_Negative() throws Exception {
        long id = -303L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(Long.toString(id));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '-303'");
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteAuthorityPerson_NotExists() throws Exception {
        Long id = 304L;
        String requestPath = ROOT + "/" + id;
        doThrow(new AuthorityPersonNotFoundException("Cannot delete not exists authority-person"))
                .when(facade).doActionAndResult(DELETE_ALL, id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(id.toString());
        verify(facade).doActionAndResult(DELETE_ALL, id);
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '304'");
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteAuthorityPerson_PersonAssignedToFaculty() throws Exception {
        Long id = 305L;
        String requestPath = ROOT + "/" + id;
        String errorMessage = "Cannot delete not free authority-person";
        doThrow(new AuthorityPersonManagesFacultyException(errorMessage)).when(facade).doActionAndResult(DELETE_ALL, id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(id.toString());
        verify(facade).doActionAndResult(DELETE_ALL, id);
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
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
}

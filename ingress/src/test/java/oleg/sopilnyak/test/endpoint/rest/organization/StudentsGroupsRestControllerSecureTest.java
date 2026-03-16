package oleg.sopilnyak.test.endpoint.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;

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
class StudentsGroupsRestControllerSecureTest extends TestModelFactory {
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "school::organization::student::groups:find.All";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "school::organization::student::groups:find.By.Id";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "school::organization::student::groups:create.Or.Update";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "school::organization::student::groups:delete";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/student-groups";
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    StudentsGroupFacade facade;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @MockitoSpyBean
    @Autowired
    StudentsGroupsRestController controller;
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
    void shouldFindAllStudentsGroups() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        int groupsAmount = 5;
        Collection<StudentsGroup> studentsGroups = makeStudentsGroups(groupsAmount);
        doReturn(Set.copyOf(studentsGroups)).when(persistenceFacade).findAllStudentsGroups();

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
        verify(facade).doActionAndResult(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        String responseString = result.getResponse().getContentAsString();

        List<StudentsGroup> studentsGroupList = MAPPER.readValue(responseString, new TypeReference<List<StudentsGroupDto>>() {
        }).stream().map(StudentsGroup.class::cast).toList();

        assertThat(studentsGroupList).hasSameSizeAs(studentsGroups).hasSize(groupsAmount);
        assertStudentsGroupLists(studentsGroups.stream().toList(), studentsGroupList);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindAllStudentsGroups_WrongPermissions() throws Exception {
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
    void shouldFindStudentsGroupById() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        Long id = 500L;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        doReturn(Optional.of(studentsGroup)).when(persistenceFacade).findStudentsGroupById(id);
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
        verify(facade).doActionAndResult(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID, id);
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindStudentsGroupById_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 5001L;
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
    void shouldCreateStudentsGroup() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_CREATE, Permission.ORG_GET));
        // prepare the test
        StudentsGroup studentsGroup = makeTestStudentsGroup(null);
        doAnswer(invocation -> {
            StudentsGroup received = invocation.getArgument(1);
            assertThat(received.getId()).isNull();
            assertStudentsGroupEquals(studentsGroup, received);
            return Optional.of(studentsGroup);
        }).when(facade).doActionAndResult(eq(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE), any(StudentsGroup.class));
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
        verify(facade).doActionAndResult(eq(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE), any(StudentsGroup.class));
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotCreateStudentsGroup_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        StudentsGroup studentsGroup = makeTestStudentsGroup(null);
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
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
        verify(controller, never()).create(any(StudentsGroupDto.class));
    }

    @Test
    void shouldUpdateStudentsGroup() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_UPDATE, Permission.ORG_GET));
        // prepare the test
        Long id = 501L;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        doAnswer(invocation -> {
            StudentsGroup received = invocation.getArgument(1);
            assertThat(received.getId()).isEqualTo(id);
            assertStudentsGroupEquals(studentsGroup, received);
            return Optional.of(studentsGroup);
        }).when(facade).doActionAndResult(eq(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE), any(StudentsGroup.class));
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
        verify(facade).doActionAndResult(eq(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE), any(StudentsGroup.class));
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateStudentsGroup_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 5011L;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

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
        verify(controller, never()).update(any(StudentsGroupDto.class));
    }

    @Test
    void shouldDeleteStudentsGroup() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_DELETE));
        // prepare the test
        Long id = 510L;
        doReturn(Optional.of(mock(StudentsGroup.class))).when(persistenceFacade).findStudentsGroupById(id);
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        verify(facade).doActionAndResult(ORGANIZATION_STUDENTS_GROUP_DELETE, id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteStudentsGroup_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 5101L;
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
        verify(controller, never()).delete(anyString());
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsGroupsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsGroupsRestController.class);
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

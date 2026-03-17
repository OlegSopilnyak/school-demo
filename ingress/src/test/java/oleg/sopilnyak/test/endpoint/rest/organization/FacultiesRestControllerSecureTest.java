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
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;
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
class FacultiesRestControllerSecureTest extends TestModelFactory {
    private static final String FACULTIES_FIND_ALL = "school::organization::faculties:find.All";
    private static final String FACULTIES_FIND_BY_ID = "school::organization::faculties:find.By.Id";
    private static final String FACULTIES_CREATE_OR_UPDATE = "school::organization::faculties:create.Or.Update";
    private static final String FACULTIES_DELETE = "school::organization::faculties:delete";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/faculties";

    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    FacultyFacade facade;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @MockitoSpyBean
    @Autowired
    FacultiesRestController controller;
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
    void shouldFindAllFaculties() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        int personsAmount = 10;
        Collection<Faculty> faculties = makeFaculties(personsAmount);
        doReturn(Set.copyOf(faculties)).when(persistenceFacade).findAllFaculties();

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
        verify(facade).doActionAndResult(FACULTIES_FIND_ALL);

        List<Faculty> facultyList =
                MAPPER.readValue(result.getResponse().getContentAsString(),
                        new TypeReference<List<FacultyDto>>() {
                        }).stream().map(Faculty.class::cast).toList();

        assertThat(facultyList).hasSize(personsAmount);
        assertFacultyLists(faculties.stream().toList(), facultyList, false);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindAllFaculties_WrongPermissions() throws Exception {
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
    void shouldFindFacultyById() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST, Permission.ORG_GET));
        // prepare the test
        Long id = 400L;
        Faculty faculty = makeTestFaculty(id);
        doReturn(Optional.of(faculty)).when(persistenceFacade).findFacultyById(id);
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
        verify(facade).doActionAndResult(FACULTIES_FIND_BY_ID, id);

        Faculty facultyDto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, facultyDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindFacultyById_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 4001L;
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
    void shouldCreateFaculty() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_CREATE, Permission.ORG_GET));
        // prepare the test
        Faculty faculty = makeTestFaculty(null);
        doAnswer(invocation -> {
            Faculty received = invocation.getArgument(1);
            assertThat(received.getId()).isNull();
            assertFacultyEquals(faculty, received);
            return Optional.of(faculty);
        }).when(facade).doActionAndResult(eq(FACULTIES_CREATE_OR_UPDATE), any(Faculty.class));
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
        verify(facade).doActionAndResult(eq(FACULTIES_CREATE_OR_UPDATE), any(Faculty.class));

        Faculty facultyDto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, facultyDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotCreateFaculty_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        Faculty faculty = makeTestFaculty(null);
        String jsonContent = MAPPER.writeValueAsString(faculty);

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
        verify(controller, never()).create(any(FacultyDto.class));
    }

    @Test
    void shouldUpdateFaculty() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_UPDATE, Permission.ORG_GET));
        // prepare the test
        Long id = 402L;
        Faculty faculty = makeTestFaculty(id);
        doAnswer(invocation -> {
            Faculty received = invocation.getArgument(1);
            assertThat(received.getId()).isEqualTo(id);
            assertFacultyEquals(faculty, received);
            return Optional.of(faculty);
        }).when(facade).doActionAndResult(eq(FACULTIES_CREATE_OR_UPDATE), any(Faculty.class));
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
        verify(facade).doActionAndResult(eq(FACULTIES_CREATE_OR_UPDATE), any(Faculty.class));

        Faculty facultyDto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, facultyDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateFaculty_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 4021L;
        Faculty faculty = makeTestFaculty(id);
        String jsonContent = MAPPER.writeValueAsString(faculty);

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
        verify(controller, never()).update(any(FacultyDto.class));
    }

    @Test
    void shouldDeleteFaculty() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_DELETE, Permission.ORG_GET));
        // prepare the test
        Long id = 410L;
        doReturn(Optional.of(mock(Faculty.class))).when(persistenceFacade).findFacultyById(id);
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteById(id.toString());
        verify(facade).doActionAndResult(FACULTIES_DELETE, id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteFaculty_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        long id = 4101L;
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
        verify(controller, never()).deleteById(anyString());
    }

    @Test
    void shouldDeleteFacultyInstance() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_DELETE));
        // prepare the test
        Long id = 411L;
        Faculty faculty = makeTestFaculty(id);
        if (faculty instanceof FakeFaculty fake) {
            fake.setCourses(List.of());
        } else {
            fail("Wrong type of the %s", faculty.toString());
        }
        doReturn(Optional.of(faculty)).when(persistenceFacade).findFacultyById(id);
        String jsonContent = MAPPER.writeValueAsString(faculty);

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(ROOT)
                                .header("Authorization", "Bearer " + credentials.getToken())
                                .content(jsonContent)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(any(FacultyDto.class));
        verify(facade).doActionAndResult(FACULTIES_DELETE, id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteFacultyInstance_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.ORG_LIST));
        // prepare the test
        Long id = 4111L;
        Faculty faculty = makeTestFaculty(id);
        if (faculty instanceof FakeFaculty fake) {
            fake.setCourses(List.of());
        } else {
            fail("Wrong type of the %s", faculty.toString());
        }
        String jsonContent = MAPPER.writeValueAsString(faculty);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(ROOT)
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
        verify(controller, never()).delete(any(FacultyDto.class));
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(FacultiesRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(FacultiesRestController.class);
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

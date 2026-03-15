package oleg.sopilnyak.test.endpoint.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
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
class StudentsRestControllerSecureTest extends TestModelFactory {
    private static final String STUDENT_FIND_BY_ID = "school::education::students:find.By.Id";
    private static final String STUDENT_FIND_ENROLLED_TO = "school::education::students:find.Enrolled.To.The.Course";
    private static final String STUDENT_FIND_NOT_ENROLLED = "school::education::students:find.Not.Enrolled.To.Any.Course";
    private static final String STUDENT_CREATE_OR_UPDATE = "school::education::students:create.Or.Update";
    private static final String STUDENT_CREATE_NEW = "school::education::students:create.Macro";
    private static final String STUDENT_DELETE_ALL = "school::education::students:delete.Macro";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/students";

    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    StudentsFacade facade;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @MockitoSpyBean
    @Autowired
    StudentsRestController controller;
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
    void shouldFindStudent() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_GET));
        // prepare the test
        Long id = 100L;
        Student student = makeTestStudent(id);
        doReturn(Optional.of(student)).when(facade).doActionAndResult(STUDENT_FIND_BY_ID, id);
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

        // check the results
        StudentDto studentDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentDto.class);
        assertThat(id).isEqualTo(studentDto.getId());
        assertThat(student.getFirstName()).isEqualTo(studentDto.getFirstName());
        assertThat(student.getLastName()).isEqualTo(studentDto.getLastName());
        assertThat(student.getGender()).isEqualTo(studentDto.getGender());
        assertThat(student.getDescription()).isEqualTo(studentDto.getDescription());
        assertCourseLists(student.getCourses(), studentDto.getCourses());
        // check the behavior
        verify(controller).findStudent(id.toString());
        checkControllerAspect();
    }

    @Test
    void shouldNotFindStudent_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_LIST));
        // prepare the test
        long id = 1001L;
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
        verify(controller, never()).findStudent(anyString());
    }

    @Test
    void shouldFindStudentsEnrolledForCourse() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_LIST, Permission.EDU_GET));
        // prepare the test
        Long courseId = 200L;
        long studentsAmount = 40L;
        Set<Student> students = LongStream.range(0, studentsAmount).mapToObj(this::makeTestStudent).collect(Collectors.toSet());
        List<Student> selectedStudentsList = students.stream().sorted(Comparator.comparing(Student::getId)).toList();
        doReturn(students).when(facade).doActionAndResult(STUDENT_FIND_ENROLLED_TO, courseId);
        String requestPath = ROOT + "/enrolled/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        List<Student> receivedStudentsList = MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
        }).stream().map(Student.class::cast).sorted(Comparator.comparing(Student::getId)).toList();
        assertThat(receivedStudentsList).hasSameSizeAs(students).hasSize((int) studentsAmount);
        assertStudentLists(selectedStudentsList, receivedStudentsList);
        // check the behavior
        verify(controller).findEnrolledTo(courseId.toString());
        checkControllerAspect();
    }

    @Test
    void shouldNotFindStudentsEnrolledForCourse_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_LIST));
        // prepare the test
        long courseId = 2001L;
        String requestPath = ROOT + "/enrolled/" + courseId;

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
        verify(controller, never()).findEnrolledTo(anyString());
    }

    @Test
    void shouldFindStudentsWithEmptyCourses() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_LIST, Permission.EDU_GET));
        // prepare the test
        long studentsAmount = 5L;
        Set<Student> students = LongStream.range(0, studentsAmount).mapToObj(this::makeTestStudent).collect(Collectors.toSet());
        List<Student> selectedStudentsList = students.stream().sorted(Comparator.comparing(Student::getId)).toList();
        doReturn(students).when(facade).doActionAndResult(STUDENT_FIND_NOT_ENROLLED);
        String requestPath = ROOT + "/empty";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        // check the results
        String responseString = result.getResponse().getContentAsString();
        List<Student> receivedStudentsList = MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
        }).stream().map(Student.class::cast).sorted(Comparator.comparing(Student::getId)).toList();
        assertThat(receivedStudentsList).hasSameSizeAs(students).hasSize((int) studentsAmount);
        assertStudentLists(selectedStudentsList, receivedStudentsList);
        // check the behavior
        verify(controller).findNotEnrolledStudents();
        checkControllerAspect();
    }

    @Test
    void shouldNotFindStudentsWithEmptyCourses_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_LIST));
        // prepare the test
        String requestPath = ROOT + "/empty";

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
        verify(controller, never()).findNotEnrolledStudents();
    }

    @Test
    void shouldCreateStudent() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE, Permission.EDU_GET));
        // prepare the test
        Student student = makeTestStudent(null);
        doAnswer(invocation -> {
            Student received = invocation.getArgument(1);
            assertThat(received.getId()).isNull();
            assertStudentEquals(student, received);
            assertCourseLists(student.getCourses(), received.getCourses());
            return Optional.of(student);
        }).when(facade).doActionAndResult(eq(STUDENT_CREATE_NEW), any(Student.class));
        String jsonContent = MAPPER.writeValueAsString(student);

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

        // check the results
        StudentDto studentDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentDto.class);
        assertThat(student.getLastName()).isEqualTo(studentDto.getLastName());
        assertThat(student.getGender()).isEqualTo(studentDto.getGender());
        assertThat(student.getDescription()).isEqualTo(studentDto.getDescription());
        assertCourseLists(student.getCourses(), studentDto.getCourses());
        // check the behavior
        verify(controller).createStudent(any(StudentDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldNotCreateStudent_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE));
        // prepare the test
        Student student = makeTestStudent(null);
        String jsonContent = MAPPER.writeValueAsString(student);

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
        verify(controller, never()).createStudent(any(StudentDto.class));
    }

    @Test
    void shouldUpdateStudent() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_UPDATE, Permission.EDU_GET));
        // prepare the test
        Long id = 101L;
        Student student = makeTestStudent(id);
        doAnswer(invocation -> {
            Student received = invocation.getArgument(1);
            assertThat(id).isEqualTo(student.getId());
            assertStudentEquals(student, received);
            assertCourseLists(student.getCourses(), received.getCourses());
            return Optional.of(student);
        }).when(facade).doActionAndResult(eq(STUDENT_CREATE_OR_UPDATE), any(Student.class));
        String jsonContent = MAPPER.writeValueAsString(student);

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

        // check the results
        StudentDto studentDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentDto.class);
        assertThat(id).isEqualTo(studentDto.getId());
        assertThat(student.getLastName()).isEqualTo(studentDto.getLastName());
        assertThat(student.getGender()).isEqualTo(studentDto.getGender());
        assertThat(student.getDescription()).isEqualTo(studentDto.getDescription());
        assertCourseLists(student.getCourses(), studentDto.getCourses());
        // check the behavior
        verify(controller).updateStudent(any(StudentDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateStudent_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_UPDATE));
        // prepare the test
        Long id = 1011L;
        Student student = makeTestStudent(id);
        String jsonContent = MAPPER.writeValueAsString(student);

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
        verify(controller, never()).updateStudent(any(StudentDto.class));
    }

    @Test
    void shouldDeleteStudentValidId() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_DELETE));
        // prepare the test
        long id = 102L;
        long profileId = id + 100;
        Student student = spy(makeClearStudent(100));
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(id);
        doReturn(profileId).when(student).getProfileId();
        StudentProfilePayload profile = mock(StudentProfilePayload.class);
        doReturn(Optional.of(profile)).when(persistenceFacade).findStudentProfileById(profileId);
        doReturn(profile).when(persistenceFacade).toEntity(profile);
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteStudent(Long.toString(id));
        verify(facade).doActionAndResult(STUDENT_DELETE_ALL, id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteStudentValidId_WrongPermissions() throws Exception {
        // signing in person with wrong permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE));
        // prepare the test
        long id = 1021L;
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
        verify(controller, never()).deleteStudent(anyString());
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsRestController.class);
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

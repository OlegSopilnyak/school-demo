package oleg.sopilnyak.test.endpoint.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;

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
class CoursesRestControllerSecureTest extends TestModelFactory {
    private static final String COURSE_CREATE_OR_UPDATE = "school::education::courses:create.Or.Update";
    private static final String COURSE_DELETE = "school::education::courses:delete";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    CoursesFacade facade;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @MockitoSpyBean
    @Autowired
    CoursesRestController controller;
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
    void shouldFindCourse() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_GET));
        // prepare the test
        Long id = 100L;
        Course course = makeTestCourse(id);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(id);
        String requestPath = "/courses/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findCourse(id.toString());
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(id).isEqualTo(courseDto.getId());
        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
        checkControllerAspect();
    }

    @Test
    void shouldNotFindCourse_WrongPermission() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE));
        // prepare the test
        long id = 1001L;
        String requestPath = "/courses/" + id;

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
        verify(controller, never()).findCourse(anyString());
    }

    @Test
    void shouldFindEnrolledFor() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_GET, Permission.EDU_LIST));
        // prepare the test
        Long studentId = 200L;
        long coursesAmount = 10L;
        Set<Course> courses = LongStream.range(0, coursesAmount).mapToObj(this::makeTestCourse).collect(Collectors.toSet());
        doReturn(courses).when(persistenceFacade).findCoursesRegisteredForStudent(studentId);
        List<Course> selectedCoursesList = courses.stream().sorted(Comparator.comparing(Course::getId)).toList();
        String requestPath = "/courses/registered/" + studentId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findRegisteredFor(studentId.toString());
        String responseString = result.getResponse().getContentAsString();
        List<Course> courseList =
                MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
                }).stream().map(Course.class::cast).sorted(Comparator.comparing(Course::getId)).toList();

        assertThat(courseList).hasSameSizeAs(courses).hasSize((int) coursesAmount);
        assertCourseLists(selectedCoursesList, courseList);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindEnrolledFor_WrongPermission() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_LIST));
        // prepare the test
        long studentId = 2001L;
        String requestPath = "/courses/registered/" + studentId;

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
        verify(controller, never()).findRegisteredFor(anyString());
    }

    @Test
    void shouldFindEmptyCourses() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_GET, Permission.EDU_LIST));
        // prepare the test
        long coursesAmount = 5L;
        Set<Course> courses = LongStream.range(0, coursesAmount).mapToObj(this::makeTestCourse).collect(Collectors.toSet());
        doReturn(courses).when(persistenceFacade).findCoursesWithoutStudents();
        List<Course> selectedCoursesList = courses.stream().sorted(Comparator.comparing(Course::getId)).toList();
        String requestPath = "/courses/empty";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findEmptyCourses();
        String responseString = result.getResponse().getContentAsString();
        List<Course> courseList =
                MAPPER.readValue(responseString, new TypeReference<List<CourseDto>>() {
                }).stream().map(Course.class::cast).sorted(Comparator.comparing(Course::getId)).toList();

        assertThat(courseList).hasSameSizeAs(courses).hasSize((int) coursesAmount);
        assertCourseLists(selectedCoursesList, courseList);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindEmptyCourses_WrongPermissions() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE));
        // prepare the test
        String requestPath = "/courses/empty";

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
        verify(controller, never()).findEmptyCourses();
    }

    @Test
    void shouldCreateCourse() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE, Permission.EDU_GET));
        // prepare the test
        Course course = makeTestCourse(null);
        doAnswer(invocation -> {
            Course received = invocation.getArgument(1);
            assertThat(received.getId()).isNull();
            assertCourseEquals(course, received);
            assertStudentLists(course.getStudents(), received.getStudents());
            return Optional.of(course);
        }).when(facade).doActionAndResult(eq(COURSE_CREATE_OR_UPDATE), any(Course.class));
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).createCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
        checkControllerAspect();
    }

    @Test
    void shouldNotCreateCourse_WrongPermissions() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE, Permission.EDU_LIST));
        // prepare the test
        Course course = makeTestCourse(null);
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
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
        verify(controller, never()).createCourse(any(CourseDto.class));
    }

    @Test
    void shouldUpdateValidCourse() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_UPDATE, Permission.EDU_GET));
        // prepare the test
        Long id = 101L;
        Course course = makeTestCourse(id);
        doAnswer(invocation -> {
            Course received = invocation.getArgument(1);
            assertThat(id).isEqualTo(received.getId());
            assertCourseEquals(course, received);
            assertStudentLists(course.getStudents(), received.getStudents());
            return Optional.of(course);
        }).when(facade).doActionAndResult(eq(COURSE_CREATE_OR_UPDATE), any(Course.class));
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        CourseDto courseDto = MAPPER.readValue(responseString, CourseDto.class);

        assertThat(course.getName()).isEqualTo(courseDto.getName());
        assertThat(course.getDescription()).isEqualTo(courseDto.getDescription());
        assertStudentLists(course.getStudents(), courseDto.getStudents());
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateValidCourse_WrongPermissions() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_UPDATE, Permission.EDU_LIST));
        // prepare the test
        Long id = 101L;
        Course course = makeTestCourse(id);
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
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
        verify(controller, never()).updateCourse(any(CourseDto.class));
    }

    @Test
    void shouldNotUpdateInvalidCourse_NullId() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_UPDATE, Permission.EDU_GET));
        // prepare the test
        Course course = makeTestCourse(null);
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: 'null'");
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateInvalidCourse_NegativeId() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_UPDATE, Permission.EDU_GET));
        // prepare the test
        Long id = -101L;
        Course course = makeTestCourse(id);
        String requestPath = "/courses";
        String jsonContent = MAPPER.writeValueAsString(course);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateCourse(any(CourseDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '-101'");
        checkControllerAspect();
    }

    @Test
    void shouldDeleteCourseValidId() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_DELETE));
        // prepare the test
        long id = 102L;
        doReturn(Optional.of(mock(Course.class))).when(persistenceFacade).findCourseById(id);
        String requestPath = "/courses/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteCourse(Long.toString(id));
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteCourseValidId_WrongPermissions() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_CREATE));
        // prepare the test
        long id = 102L;
        doReturn(Optional.of(mock(Course.class))).when(persistenceFacade).findCourseById(id);
        String requestPath = "/courses/" + id;
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
        verify(controller, never()).deleteCourse(anyString());
    }

    @Test
    void shouldNotDeleteCourseValidId_CourseNotExistsException() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_DELETE));
        // prepare the test
        Long id = 103L;
        String requestPath = "/courses/" + id;
        var exception = new CourseNotFoundException("Wrong course");
        doThrow(exception).when(persistenceFacade).findCourseById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(id.toString());
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong course-id: '103'");
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteCourseValidId_CourseWithStudentsException() throws Exception {
        // signing in person with proper permissions
        AccessCredentials credentials = signInWith(List.of(Permission.EDU_DELETE));
        // prepare the test
        Long id = 104L;
        String requestPath = "/courses/" + id;
        String errorMessage = "Wrong course";
        var exception = new CourseWithStudentsException(errorMessage);
        doThrow(exception).when(facade).doActionAndResult(COURSE_DELETE, id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteCourse(id.toString());
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(CoursesRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(CoursesRestController.class);
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

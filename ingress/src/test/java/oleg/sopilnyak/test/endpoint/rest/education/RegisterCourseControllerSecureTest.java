package oleg.sopilnyak.test.endpoint.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
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
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
@WebAppConfiguration
class RegisterCourseControllerSecureTest extends TestModelFactory {
    private static final String COURSE_REGISTER = "school::education::courses:register";
    private static final String COURSE_UN_REGISTER = "school::education::courses:unregister";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/register/";
    @Mock
    Student student;
    @Mock
    Course course;
    @Captor
    ArgumentCaptor<Student> studentCapture;
    @Captor
    ArgumentCaptor<Course> courseCapture;

    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    CoursesFacade coursesFacade;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @MockitoSpyBean
    @Autowired
    RegisterCourseController controller;
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
    void shouldRegisterCourse() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(Permission.EDU_UPDATE);
        // prepare the test
        Long studentId = 100L;
        Long courseId = 200L;
        // prepare action's dataset
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);
        checkControllerAspect();
    }

    @Test
    void shouldNotRegisterCourse_WrongPermission() throws Exception {
        // signing in person with wrong permission
        AccessCredentials credentials = signInWith(Permission.EDU_GET);
        // prepare the test
        long studentId = 101L;
        long courseId = 201L;
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isUnauthorized())
                .andDo(print());

        verify(controller, never()).registerToCourse(anyString(), anyString());
    }

    @Test
    void shouldNotRegisterCourse_NoRoomInTheCourseException() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(Permission.EDU_UPDATE);
        // prepare the test
        Long studentId = 102L;
        Long courseId = 202L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String errorMessage = "No free rooms for the student";
        var exception = new CourseHasNoRoomException(errorMessage);
        doThrow(exception).when(persistenceFacade).link(student, course);
        String requestPath = ROOT + studentId + "/to/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);

        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
        checkControllerAspect();
    }

    @Test
    void shouldNotRegisterCourse_StudentCoursesExceedException() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(Permission.EDU_UPDATE);
        // prepare the test
        Long studentId = 103L;
        Long courseId = 203L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String errorMessage = "Too many courses for the student";
        var exception = new StudentCoursesExceedException(errorMessage);
        doThrow(exception).when(persistenceFacade).link(student, course);

        String requestPath = ROOT + studentId + "/to/" + courseId;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .header("Authorization", "Bearer " + credentials.getToken())
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).registerToCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);

        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
        checkControllerAspect();
    }

    @Test
    void shouldUnRegisterCourse() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(Permission.EDU_UPDATE);
        // prepare the test
        Long studentId = 104L;
        Long courseId = 204L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        doReturn(Optional.of(student)).when(persistenceFacade).findStudentById(studentId);
        doReturn(Optional.of(course)).when(persistenceFacade).findCourseById(courseId);
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).unRegisterCourse(studentId.toString(), courseId.toString());
        verify(coursesFacade).doActionAndResult(eq(COURSE_UN_REGISTER), studentCapture.capture(), courseCapture.capture());
        assertThat(studentCapture.getValue().getId()).isEqualTo(studentId);
        assertThat(courseCapture.getValue().getId()).isEqualTo(courseId);
        checkControllerAspect();
    }

    @Test
    void shouldNotUnRegisterCourse_WrongPermission() throws Exception {
        // signing in person with proper permission
        AccessCredentials credentials = signInWith(Permission.EDU_DELETE);
        // prepare the test
        long studentId = 105L;
        long courseId = 205L;
        String requestPath = ROOT + studentId + "/to/" + courseId;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .header("Authorization", "Bearer " + credentials.getToken())
                )
                .andExpect(status().isUnauthorized())
                .andDo(print());

        verify(controller, never()).unRegisterCourse(anyString(), anyString());
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(RegisterCourseController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(RegisterCourseController.class);
    }

    private AccessCredentials signInWith(Permission permission) throws Exception {
        String username = UUID.randomUUID().toString();
        String password = "password";
        // prepare dataset
        mockingDataSet(username, password, permission);
        // signing in the person
        Optional<AccessCredentials> credentials = authenticationFacade.signIn(username, password);
        assertThat(credentials).isPresent();
        return credentials.orElseThrow();
    }

    private void mockingDataSet(String username, String password, Permission permission) throws Exception {
        Long personId = 1L;
        Long profileId = 2L;
        PrincipalProfile profile = makePrincipalProfile(profileId);
        if (profile instanceof TestModelFactory.FakePrincipalProfile fakeProfile) {
            fakeProfile.setUsername(username);
            fakeProfile.setSignature(profile.makeSignatureFor(password));
            fakeProfile.setPermissions(Set.of(permission));
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

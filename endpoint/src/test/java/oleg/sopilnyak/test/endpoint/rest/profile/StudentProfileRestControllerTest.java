package oleg.sopilnyak.test.endpoint.rest.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
class StudentProfileRestControllerTest extends TestModelFactory {
    private static final String ROOT = "/profiles/students";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
    @MockBean
    PersistenceFacade persistenceFacade;
    @SpyBean
    @Autowired
    StudentProfileFacade facade;
    @SpyBean
    @Autowired
    StudentProfileRestController controller;
    @SpyBean
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
    void shouldFindStudentProfile() throws Exception {
        Long id = 401L;
        String requestPath = ROOT + "/" + id;
        StudentProfile profile = makeStudentProfile(id);
        doReturn(Optional.of(profile)).when(persistenceFacade).findStudentProfileById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id.toString());
        verify(facade).findStudentProfileById(id);
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertProfilesEquals(profile, dto);
        checkControllerAspect();
    }

    @Test
    void shouldNotFoundStudentProfile_NegativeId() throws Exception {
        long id = -401L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(Long.toString(id));
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -401 is not found");
        checkControllerAspect();
    }

    @Test
    void shouldNotFoundStudentProfile_WrongId() throws Exception {
        Long id = 401L;
        String requestPath = ROOT + "/" + id + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id + "!");
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '401!'");
        checkControllerAspect();
    }

    @Test
    void shouldUpdateStudentProfile() throws Exception {
        Long id = 404L;
        StudentProfile profile = makeStudentProfile(id);
        doAnswer(invocation -> {
            StudentProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(received, profile);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade).createOrUpdateProfile(any(StudentProfile.class));

        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertProfilesEquals(dto, profile);
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateStudentProfile_NullId() throws Exception {
        StudentProfile profile = makeStudentProfile(null);
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(StudentProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: 'null'");
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateStudentProfile_NegativeId() throws Exception {
        Long id = -404L;
        StudentProfile profile = makeStudentProfile(id);
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(StudentProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '-404'");
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateStudentProfile_ExceptionThrown() throws Exception {
        Long id = 404L;
        StudentProfile profile = makeStudentProfile(id);
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        String message = "Cannot update student profile: '404!'";
        doThrow(new RuntimeException(message)).when(facade).createOrUpdateProfile(any(StudentProfile.class));

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isInternalServerError())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade).createOrUpdateProfile(any(StudentProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(500);
        assertThat(error.getErrorMessage()).isEqualTo(message);
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentProfileRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentProfileRestController.class);
    }
}

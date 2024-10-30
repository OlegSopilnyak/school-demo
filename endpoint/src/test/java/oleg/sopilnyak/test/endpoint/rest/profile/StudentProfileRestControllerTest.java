package oleg.sopilnyak.test.endpoint.rest.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.profile.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class StudentProfileRestControllerTest extends TestModelFactory {
    private static final String ROOT = "/profiles/students";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
    @Mock
    StudentProfileFacade facade;
    @Spy
    @InjectMocks
    StudentProfileRestController controller;

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
        when(facade.findStudentProfileById(id)).thenReturn(Optional.of(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id.toString());
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertThat(id).isEqualTo(dto.getId());
        assertThat(profile.getPhotoUrl()).isEqualTo(dto.getPhotoUrl());
        assertThat(profile.getEmail()).isEqualTo(dto.getEmail());
        assertThat(profile.getPhone()).isEqualTo(dto.getPhone());
        assertThat(profile.getLocation()).isEqualTo(dto.getLocation());
        assertThat(profile.getExtraKeys()).isEqualTo(dto.getExtraKeys());
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
    }
}
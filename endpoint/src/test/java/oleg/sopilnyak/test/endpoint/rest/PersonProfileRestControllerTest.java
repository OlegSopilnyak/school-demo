package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.facade.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class PersonProfileRestControllerTest extends TestModelFactory {
    private final static String ROOT = "/profiles";
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
    @Mock
    PersonProfileFacade facade;
    @Spy
    @InjectMocks
    PersonProfileRestController controller;

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
        StudentProfile profile = makeStudentProfile(id);
        when(facade.findStudentProfileById(id)).thenReturn(Optional.of(profile));
        String requestPath = ROOT + "/students/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findStudentProfile(id.toString());
        StudentProfileDto dto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertThat(id).isEqualTo(dto.getId());
        assertThat(profile.getPhotoUrl()).isEqualTo(dto.getPhotoUrl());
        assertThat(profile.getEmail()).isEqualTo(dto.getEmail());
        assertThat(profile.getPhone()).isEqualTo(dto.getPhone());
        assertThat(profile.getLocation()).isEqualTo(dto.getLocation());
        assertThat(profile.getExtraKeys()).isEqualTo(dto.getExtraKeys());
    }

    @Test
    void shouldNotFoundStudentProfile_NotExists() throws Exception {
        Long id = -401L;
        String requestPath = ROOT + "/students/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findStudentProfile(id.toString());
        String response = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error =
                MAPPER.readValue(response, RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -401 is not found");
    }

    @Test
    void shouldNotFoundStudentProfile_WrongId() throws Exception {
        Long id = -401L;
        String requestPath = ROOT + "/students/" + id + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findStudentProfile(id + "!");
        String response = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error =
                MAPPER.readValue(response, RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '-401!'");
    }

    @Test
    void shouldFindPrincipalProfile() throws Exception {
        Long id = 402L;
        PrincipalProfile profile = makePrincipalProfile(id);
        when(facade.findPrincipalProfileById(id)).thenReturn(Optional.of(profile));
        String requestPath = ROOT + "/principals/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findPrincipalProfile(id.toString());
        PrincipalProfileDto dto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertThat(id).isEqualTo(dto.getId());
        assertThat(profile.getPhotoUrl()).isEqualTo(dto.getPhotoUrl());
        assertThat(profile.getEmail()).isEqualTo(dto.getEmail());
        assertThat(profile.getPhone()).isEqualTo(dto.getPhone());
        assertThat(profile.getLocation()).isEqualTo(dto.getLocation());
        assertThat(profile.getExtraKeys()).isEqualTo(dto.getExtraKeys());
        assertThat(profile.getLogin()).isEqualTo(dto.getLogin());
    }

    @Test
    void shouldNotFoundPrincipalProfile_NotExists() throws Exception {
        Long id = -402L;
        String requestPath = ROOT + "/principals/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findPrincipalProfile(id.toString());
        String response = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error =
                MAPPER.readValue(response, RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -402 is not found");
    }

    @Test
    void shouldNotFoundPrincipalProfile_WrongId() throws Exception {
        Long id = -402L;
        String requestPath = ROOT + "/principals/" + id + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findPrincipalProfile(id + "!");
        String response = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error =
                MAPPER.readValue(response, RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-402!'");
    }

    @Test
    void shouldCreateStudentProfile() throws Exception {
        Long id = 403L;
        StudentProfile profile = makeStudentProfile(id);
        String requestPath = ROOT + "/students";
        doAnswer(invocation -> {
            StudentProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertProfilesEquals(profile, received, false);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(StudentProfileDto.class));
        verify(facade).createOrUpdateProfile(any(StudentProfile.class));

        StudentProfileDto profileDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertProfilesEquals(profile, profileDto, false);
    }

    @Test
    void shouldNotCreateStudentProfile() throws Exception {
        Long id = 403L;
        StudentProfile profile = makeStudentProfile(id);
        when(facade.createOrUpdateProfile(any(StudentProfile.class))).thenThrow(new RuntimeException());
        String requestPath = ROOT + "/students";
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isInternalServerError())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(StudentProfileDto.class));
        verify(facade).createOrUpdateProfile(any(StudentProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(500);
        assertThat(error.getErrorMessage()).startsWith("Cannot create new student-profile");
    }
}
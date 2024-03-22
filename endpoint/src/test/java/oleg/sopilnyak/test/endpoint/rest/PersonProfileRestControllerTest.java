package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
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
import org.springframework.http.HttpStatus;
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
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
            assertProfilesEquals(received, profile, false);
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
        assertProfilesEquals(profileDto, profile,  false);
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

    @Test
    void shouldUpdateStudentProfile() throws Exception {
        Long id = 404L;
        StudentProfile profile = makeStudentProfile(id);
        String requestPath = ROOT + "/students";
        doAnswer(invocation -> {
            StudentProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(received, profile);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade).createOrUpdateProfile(any(StudentProfile.class));

        StudentProfileDto profileDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertProfilesEquals(profileDto, profile);
    }

    @Test
    void shouldNotUpdateStudentProfile_NullId() throws Exception {
        Long id = 404L;
        StudentProfile profile = makeStudentProfile(null);
        String requestPath = ROOT + "/students";
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(StudentProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: 'null'");
    }

    @Test
    void shouldNotUpdateStudentProfile_NegativeId() throws Exception {
        Long id = -404L;
        StudentProfile profile = makeStudentProfile(id);
        String requestPath = ROOT + "/students";
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(StudentProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '-404'");
    }

    @Test
    void shouldNotUpdateStudentProfile_ExceptionThrown() throws Exception {
        Long id = 404L;
        StudentProfile profile = makeStudentProfile(id);
        String requestPath = ROOT + "/students";
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        when(facade.createOrUpdateProfile(any(StudentProfile.class))).thenThrow(new RuntimeException());

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isInternalServerError())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade).createOrUpdateProfile(any(StudentProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(500);
        assertThat(error.getErrorMessage()).startsWith("Cannot update student-profile");
    }

    @Test
    void shouldCreatePrincipalProfile() throws Exception {
        Long id = 405L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        doAnswer(invocation -> {
            PrincipalProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertProfilesEquals(received, profile, false);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(PrincipalProfile.class));
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

        verify(controller).create(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        PrincipalProfileDto profileDto = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                PrincipalProfileDto.class);
        assertProfilesEquals(profileDto, profile, false);
    }

    @Test
    void shouldNotCreatePrincipalProfile() throws Exception {
        Long id = 405L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        when(facade.createOrUpdateProfile(any(PrincipalProfile.class))).thenThrow(new RuntimeException());
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

        verify(controller).create(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(500);
        assertThat(error.getErrorMessage()).startsWith("Cannot create new principal-profile");
    }

    @Test
    void shouldUpdatePrincipalProfile() throws Exception {
        Long id = 406L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        doAnswer(invocation -> {
            PrincipalProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(profile, received);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(PrincipalProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        PrincipalProfileDto profileDto = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                PrincipalProfileDto.class);
        assertProfilesEquals(profileDto, profile);
    }

    @Test
    void shouldNotUpdatePrincipalProfile_NullId() throws Exception {
        Long id = 406L;
        PrincipalProfile profile = makePrincipalProfile(null);
        String requestPath = ROOT + "/principals";
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(PrincipalProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: 'null'");
    }

    @Test
    void shouldNotUpdatePrincipalProfile_NegativeId() throws Exception {
        Long id = -406L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(PrincipalProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-406'");
    }

    @Test
    void shouldNotUpdatePrincipalProfile_ExceptionThrown() throws Exception {
        Long id = 406L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        when(facade.createOrUpdateProfile(any(PrincipalProfile.class))).thenThrow(new RuntimeException());

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isInternalServerError())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(error.getErrorMessage()).startsWith("Cannot update principal-profile Principal");
    }

    @Test
    void shouldDeleteStudentProfile() throws Exception {
        Long id = 407L;
        StudentProfile profile = makeStudentProfile(id);
        String requestPath = ROOT + "/students";
        doAnswer(invocation -> {
            StudentProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(received, profile);
            return null;
        }).when(facade).deleteProfile(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .content(jsonContent)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).deleteStudentProfile(any(StudentProfileDto.class));
        verify(facade).deleteProfile(any(StudentProfile.class));
    }

    @Test
    void shouldNotDeleteStudentProfile_ProfileNotExistsExceptionThrown() throws Exception {
        Long id = 407L;
        StudentProfile profile = makeStudentProfile(null);
        String requestPath = ROOT + "/students";
        doThrow(new ProfileNotExistsException("")).when(facade).deleteProfile(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudentProfile(any(StudentProfileDto.class));
        verify(facade).deleteProfile(any(StudentProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).startsWith("Wrong student-profile to delete Person");
    }

    @Test
    void shouldNotDeleteStudentProfile_RuntimeExceptionThrown() throws Exception {
        Long id = 407L;
        StudentProfile profile = makeStudentProfile(id);
        String requestPath = ROOT + "/students";
        doThrow(new RuntimeException("")).when(facade).deleteProfile(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudentProfile(any(StudentProfileDto.class));
        verify(facade).deleteProfile(any(StudentProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).startsWith("Cannot delete student-profile Person");
    }

    @Test
    void shouldDeletePrincipalProfile() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        doAnswer(invocation -> {
            PrincipalProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(received, profile);
            return null;
        }).when(facade).deleteProfile(any(PrincipalProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .content(jsonContent)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).deletePrincipalProfile(any(PrincipalProfileDto.class));
        verify(facade).deleteProfile(any(PrincipalProfile.class));
    }

    @Test
    void shouldDeletePrincipalProfile_ProfileNotExistsExceptionThrown() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        doThrow(new ProfileNotExistsException("")).when(facade).deleteProfile(any(PrincipalProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePrincipalProfile(any(PrincipalProfileDto.class));
        verify(facade).deleteProfile(any(PrincipalProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).startsWith("Wrong principal-profile to delete Principal");
    }

    @Test
    void shouldDeletePrincipalProfile_RuntimeExceptionThrown() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT + "/principals";
        doThrow(new RuntimeException("")).when(facade).deleteProfile(any(PrincipalProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePrincipalProfile(any(PrincipalProfileDto.class));
        verify(facade).deleteProfile(any(PrincipalProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).startsWith("Cannot delete principal-profile Principal");
    }

    @Test
    void shouldDeleteStudentProfileById() throws Exception {
        long id = 409L;
        String requestPath = ROOT + "/students/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).deleteStudentProfile(String.valueOf(id));
        verify(facade).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteStudentProfileById_NullId() throws Exception {
        long id = 409L;
        String requestPath = ROOT + "/students/" + null;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudentProfile("null");
        verify(facade, never()).deleteProfileById(null);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: 'null'");
    }

    @Test
    void shouldNotDeleteStudentProfileById_WrongId() throws Exception {
        long id = 409L;
        String requestPath = ROOT + "/students/" + id + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudentProfile("409!");
        verify(facade, never()).deleteProfileById(anyLong());

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '409!'");
    }

    @Test
    void shouldNotDeleteStudentProfileById_NegativeId() throws Exception {
        long id = -409L;
        String requestPath = ROOT + "/students/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudentProfile(String.valueOf(id));
        verify(facade, never()).deleteProfileById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '-409'");
    }

    @Test
    void shouldNotDeleteStudentProfileById_ExceptionThrown() throws Exception {
        long id = 409L;
        String requestPath = ROOT + "/students/" + id;
        doThrow(new RuntimeException("")).when(facade).deleteProfileById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudentProfile(String.valueOf(id));
        verify(facade).deleteProfileById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).isEqualTo("Cannot delete student-profile for id = 409");
    }

    @Test
    void shouldDeletePrincipalProfileById() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/principals/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).deletePrincipalProfile(String.valueOf(id));
        verify(facade).deleteProfileById(id);
    }

    @Test
    void shouldNotDeletePrincipalProfileById_NullId() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/principals/" + null;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePrincipalProfile("null");
        verify(facade, never()).deleteProfileById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: 'null'");
    }

    @Test
    void shouldNotDeletePrincipalProfileById_WrongId() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/principals/" + id + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePrincipalProfile("410!");
        verify(facade, never()).deleteProfileById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '410!'");
    }

    @Test
    void shouldNotDeletePrincipalProfileById_NegativeId() throws Exception {
        long id = -410L;
        String requestPath = ROOT + "/principals/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePrincipalProfile(String.valueOf(id));
        verify(facade, never()).deleteProfileById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-410'");
    }

    @Test
    void shouldNotDeletePrincipalProfileById_ExceptionThrown() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/principals/" + id;
        doThrow(new RuntimeException("")).when(facade).deleteProfileById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePrincipalProfile(String.valueOf(id));
        verify(facade).deleteProfileById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).isEqualTo("Cannot delete principal-profile for id = 410");
    }
}
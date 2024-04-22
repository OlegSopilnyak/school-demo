package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
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
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class StudentProfileRestControllerTest extends TestModelFactory {
    private final static String ROOT = "/profiles/students";
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
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
        String requestPath = rootWithId(id);
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
        StudentProfileDto dto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertThat(id).isEqualTo(dto.getId());
        assertThat(profile.getPhotoUrl()).isEqualTo(dto.getPhotoUrl());
        assertThat(profile.getEmail()).isEqualTo(dto.getEmail());
        assertThat(profile.getPhone()).isEqualTo(dto.getPhone());
        assertThat(profile.getLocation()).isEqualTo(dto.getLocation());
        assertThat(profile.getExtraKeys()).isEqualTo(dto.getExtraKeys());
    }

    @Test
    void shouldNotFoundStudentProfile_NegativeId() throws Exception {
        Long id = -401L;
        String requestPath = rootWithId(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id.toString());
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -401 is not found");
    }

    @Test
    void shouldNotFoundStudentProfile_WrongId() throws Exception {
        Long id = 401L;
        String requestPath = rootWithId(id) + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id + "!");
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '401!'");
    }

    @Test
    void shouldCreateStudentProfile() throws Exception {
        Long id = 403L;
        StudentProfile profile = makeStudentProfile(id);
        doAnswer(invocation -> {
            StudentProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertProfilesEquals(received, profile, false);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
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
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
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

        StudentProfileDto profileDto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertProfilesEquals(profileDto, profile);
    }

    @Test
    void shouldNotUpdateStudentProfile_NullId() throws Exception {
        Long id = 404L;
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
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        when(facade.createOrUpdateProfile(any(StudentProfile.class))).thenThrow(new RuntimeException());

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

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(500);
        assertThat(error.getErrorMessage()).startsWith("Cannot update student-profile");
    }

    @Test
    void shouldDeleteStudentProfile() throws Exception {
        Long id = 407L;
        StudentProfile profile = makeStudentProfile(id);
        doAnswer(invocation -> {
            StudentProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(received, profile);
            return null;
        }).when(facade).delete(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(ROOT)
                                .content(jsonContent)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).delete(any(StudentProfileDto.class));
        verify(facade).delete(any(StudentProfile.class));
    }

    @Test
    void shouldNotDeleteStudentProfile_ProfileNotExistsExceptionThrown() throws Exception {
        StudentProfile profile = makeStudentProfile(null);
        doThrow(new NotExistProfileException("")).when(facade).delete(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete(any(StudentProfileDto.class));
        verify(facade).delete(any(StudentProfile.class));

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
        doThrow(new RuntimeException("")).when(facade).delete(any(StudentProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete(any(StudentProfileDto.class));
        verify(facade).delete(any(StudentProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).startsWith("Cannot delete student-profile Person");
    }

    @Test
    void shouldDeleteStudentProfileById() throws Exception {
        long id = 409L;
        String requestPath = rootWithId(id);
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).deleteById(String.valueOf(id));
        verify(facade).deleteById(id);
    }

    @Test
    void shouldNotDeleteStudentProfileById_NullId() throws Exception {
        String requestPath = rootWithId(null);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteById("null");
        verify(facade, never()).deleteById(null);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: 'null'");
    }

    @Test
    void shouldNotDeleteStudentProfileById_WrongId() throws Exception {
        long id = 409L;
        String requestPath = rootWithId(id) + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteById("409!");
        verify(facade, never()).deleteById(anyLong());

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '409!'");
    }

    @Test
    void shouldNotDeleteStudentProfileById_NegativeId() throws Exception {
        long id = -409L;
        String requestPath = rootWithId(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteById(String.valueOf(id));
        verify(facade, never()).deleteById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '-409'");
    }

    @Test
    void shouldNotDeleteStudentProfileById_ExceptionThrown() throws Exception {
        long id = 409L;
        String requestPath = rootWithId(id);
        doThrow(new RuntimeException("")).when(facade).deleteById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteById(String.valueOf(id));
        verify(facade).deleteById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).isEqualTo("Cannot delete student-profile for id = 409");
    }

    private static String rootWithId(Long id){
        return ROOT + "/" + id;
    }
}
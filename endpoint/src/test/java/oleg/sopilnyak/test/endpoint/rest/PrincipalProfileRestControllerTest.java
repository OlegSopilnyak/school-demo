package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
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
class PrincipalProfileRestControllerTest extends TestModelFactory {
    private final static String ROOT = "/profiles/principals";
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
    @Mock
    PrincipalProfileFacade facade;
    @Spy
    @InjectMocks
    PrincipalProfileRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    void shouldFindPrincipalProfile() throws Exception {
        Long id = 402L;
        PrincipalProfile profile = makePrincipalProfile(id);
        when(facade.findPrincipalProfileById(id)).thenReturn(Optional.of(profile));
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id.toString());
        PrincipalProfileDto dto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertThat(dto.getId()).isEqualTo(id);
        assertProfilesEquals(dto, profile);
    }

    @Test
    void shouldNotFoundPrincipalProfile_NotExists() throws Exception {
        Long id = -402L;
        String requestPath = ROOT + "/" + id;
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
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -402 is not found");
    }

    @Test
    void shouldNotFoundPrincipalProfile_WrongId() throws Exception {
        Long id = -402L;
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-402!'");
    }

    @Test
    void shouldCreatePrincipalProfile() throws Exception {
        Long id = 405L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT;
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

        PrincipalProfileDto profileDto =
                MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertProfilesEquals(profileDto, profile, false);
    }

    @Test
    void shouldNotCreatePrincipalProfile() throws Exception {
        Long id = 405L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT;
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
        String requestPath = ROOT;
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

        PrincipalProfileDto profileDto =
                MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertProfilesEquals(profileDto, profile);
    }

    @Test
    void shouldNotUpdatePrincipalProfile_NullId() throws Exception {
        Long id = 406L;
        PrincipalProfile profile = makePrincipalProfile(null);
        String requestPath = ROOT;
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
        String requestPath = ROOT;
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
        String requestPath = ROOT;
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
    void shouldDeletePrincipalProfile() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT;
        doAnswer(invocation -> {
            PrincipalProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(received, profile);
            return null;
        }).when(facade).delete(any(PrincipalProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                                .content(jsonContent)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        verify(controller).delete(any(PrincipalProfileDto.class));
        verify(facade).delete(any(PrincipalProfile.class));
    }

    @Test
    void shouldDeletePrincipalProfile_ProfileNotExistsExceptionThrown() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String requestPath = ROOT;
        doThrow(new NotExistProfileException("")).when(facade).delete(any(PrincipalProfile.class));
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

        verify(controller).delete(any(PrincipalProfileDto.class));
        verify(facade).delete(any(PrincipalProfile.class));

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
        String requestPath = ROOT;
        doThrow(new RuntimeException("")).when(facade).delete(any(PrincipalProfile.class));
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

        verify(controller).delete(any(PrincipalProfileDto.class));
        verify(facade).delete(any(PrincipalProfile.class));

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).startsWith("Cannot delete principal-profile Principal");
    }

    @Test
    void shouldDeletePrincipalProfileById() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/" + id;
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
    void shouldNotDeletePrincipalProfileById_NullId() throws Exception {
        String requestPath = ROOT + "/" + null;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteById("null");
        verify(facade, never()).deleteById(anyLong());

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: 'null'");
    }

    @Test
    void shouldNotDeletePrincipalProfileById_WrongId() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/" + id + "!";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteById("410!");
        verify(facade, never()).deleteById(id);

        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '410!'");
    }

    @Test
    void shouldNotDeletePrincipalProfileById_NegativeId() throws Exception {
        long id = -410L;
        String requestPath = ROOT + "/" + id;
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
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-410'");
    }

    @Test
    void shouldNotDeletePrincipalProfileById_ExceptionThrown() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/" + id;
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
        assertThat(error.getErrorMessage()).isEqualTo("Cannot delete principal-profile for id = 410");
    }
}
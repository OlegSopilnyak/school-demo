package oleg.sopilnyak.test.endpoint.rest.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.profile.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
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
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class PrincipalProfileRestControllerTest extends TestModelFactory {
    private static final String ROOT = "/profiles/principals";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
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
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertThat(dto.getId()).isEqualTo(id);
        assertProfilesEquals(profile, dto);
    }

    @Test
    void shouldNotFoundPrincipalProfile_NotExists() throws Exception {
        long id = -402L;
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
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-402!'");
    }

    @Test
    void shouldCreatePrincipalProfile() throws Exception {
        Long id = 405L;
        PrincipalProfile profile = makePrincipalProfile(id);
        doAnswer(invocation -> {
            PrincipalProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertProfilesEquals(received, profile, false);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(PrincipalProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        var profileDto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertProfilesEquals(profileDto, profile, false);
    }

    @Test
    void shouldNotCreatePrincipalProfile() throws Exception {
        Long id = 405L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String message = "Cannot create principal profile: '405!'";
        Exception exception = new RuntimeException(message);
        doThrow(exception).when(facade).createOrUpdateProfile(any(PrincipalProfile.class));
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

        verify(controller).create(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(500);
        assertThat(error.getErrorMessage()).isEqualTo(message);
    }

    @Test
    void shouldUpdatePrincipalProfile() throws Exception {
        Long id = 406L;
        PrincipalProfile profile = makePrincipalProfile(id);
        doAnswer(invocation -> {
            PrincipalProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(profile, received);
            return Optional.of(received);
        }).when(facade).createOrUpdateProfile(any(PrincipalProfile.class));
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

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertProfilesEquals(profile, dto);
    }

    @Test
    void shouldNotUpdatePrincipalProfile_NullId() throws Exception {
        PrincipalProfile profile = makePrincipalProfile(null);
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

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(PrincipalProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: 'null'");
    }

    @Test
    void shouldNotUpdatePrincipalProfile_NegativeId() throws Exception {
        Long id = -406L;
        PrincipalProfile profile = makePrincipalProfile(id);
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

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(PrincipalProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-406'");
    }

    @Test
    void shouldNotUpdatePrincipalProfile_ExceptionThrown() throws Exception {
        Long id = 406L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        String message = "Cannot update principal profile: '406!'";
        Exception exception = new RuntimeException(message);
        doThrow(exception).when(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isInternalServerError())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(PrincipalProfileDto.class));
        verify(facade).createOrUpdateProfile(any(PrincipalProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(error.getErrorMessage()).isEqualTo(message);
    }

    @Test
    void shouldDeletePrincipalProfile() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        doAnswer(invocation -> {
            PrincipalProfile received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertProfilesEquals(received, profile);
            return null;
        }).when(facade).delete(any(PrincipalProfile.class));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(ROOT)
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
    void shouldNotDeletePrincipalProfile_ProfileNotExistsExceptionThrown() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String errorMessage = "Cannot delete principal profile: '408'";
        doThrow(new ProfileNotFoundException(errorMessage)).when(facade).delete(any(PrincipalProfile.class));
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

        verify(controller).delete(any(PrincipalProfileDto.class));
        verify(facade).delete(any(PrincipalProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void shouldNotDeletePrincipalProfile_GeneralCannotDeleteExceptionThrown() throws Exception {
        Long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String errorMessage = "Cannot delete principal profile: '408'";
        doThrow(new GeneralCannotDeleteException(errorMessage)).when(facade).delete(any(PrincipalProfile.class));
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

        verify(controller).delete(any(PrincipalProfileDto.class));
        verify(facade).delete(any(PrincipalProfile.class));

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
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

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
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

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
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

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-410'");
    }

    @Test
    void shouldNotDeletePrincipalProfileById_ExceptionThrown() throws Exception {
        long id = 410L;
        String requestPath = ROOT + "/" + id;
        String errorMessage = "cannot delete profile with id:" + id;
        doThrow(new GeneralCannotDeleteException(errorMessage)).when(facade).deleteById(id);
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

        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
    }
}
package oleg.sopilnyak.test.endpoint.end2end.rest.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.configuration.ActionContextReleaseInterceptor;
import oleg.sopilnyak.test.endpoint.dto.profile.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.profile.PrincipalProfileRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.PrincipalProfilePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {BusinessLogicConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class PrincipalProfileRestControllerTest extends MysqlTestModelFactory {
    private static final String ROOT = "/profiles/principals";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
    @Autowired
    PersistenceFacade database;
    @SpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @Autowired
    CommandsFactory<PrincipalProfileCommand> factory;
    @SpyBean
    @Autowired
    PrincipalProfileFacade facade;

    PrincipalProfileRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = spy(new PrincipalProfileRestController(facade));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .addInterceptors(new ActionContextReleaseInterceptor())
                .build();
    }

    @Test
    @Transactional
    void everythingShouldBeValid() {
        assertThat(factory).isNotNull();
        assertThat(mapper).isNotNull();
        assertThat(database).isNotNull();

        assertThat(facade).isNotNull();
        assertThat(factory).isEqualTo(ReflectionTestUtils.getField(facade, "factory"));
        assertThat(mapper).isEqualTo(ReflectionTestUtils.getField(facade, "mapper"));

        assertThat(controller).isNotNull();
        assertThat(facade).isEqualTo(ReflectionTestUtils.getField(controller, "facade"));
    }

    @Test
    @Transactional
    void shouldFindPrincipalProfile() throws Exception {
        var profile = getPersistent(makePrincipalProfile(null));
        long id = profile.getId();
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(String.valueOf(id));
        verify(facade).findPrincipalProfileById(id);
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertProfilesEquals(profile, dto);
    }

    @Test
    @Transactional
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
        verify(facade).findPrincipalProfileById(id);
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -402 is not found");
    }

    @Test
    @Transactional
    void shouldNotFoundPrincipalProfile_WrongId() throws Exception {
        long id = -402L;
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
        verify(facade, never()).findPrincipalProfileById(anyLong());
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-402!'");
    }

    @Test
    @Transactional
    void shouldCreatePrincipalProfile() throws Exception {
        PrincipalProfile profile = makePrincipalProfile(null);
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
    @Transactional
    void shouldNotCreatePrincipalProfile_FacadeCreateOrUpdateThrows() throws Exception {
        PrincipalProfile profile = makePrincipalProfile(null);
        String message = "Cannot create principal profile";
        doThrow(new RuntimeException(message)).when(facade).createOrUpdateProfile(any(PrincipalProfile.class));
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
    @Transactional
    void shouldUpdatePrincipalProfile() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
        String originalEmail = profile.getEmail();
        profile.setEmail(profile.getEmail() + "::" + profile.getEmail());
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
        assertThat(originalEmail).isNotEqualTo(dto.getEmail());
    }

    @Test
    @Transactional
    void shouldNotUpdatePrincipalProfile_NullId() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
        profile.setId(null);
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
    @Transactional
    void shouldNotUpdatePrincipalProfile_NegativeId() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
        profile.setId(-profile.getId());
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
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '" + profile.getId() + "'");
    }

    @Test
    @Transactional
    void shouldNotUpdatePrincipalProfile_ExceptionThrown() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
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
    @Transactional
    void shouldDeletePrincipalProfile() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
        long id = profile.getId();
        assertThat(facade.findPrincipalProfileById(id)).isPresent();
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
        assertThat(facade.findPrincipalProfileById(id)).isEmpty();
    }

    @Test
    @Transactional
    void shouldNotDeletePrincipalProfile_ProfileNotExistsExceptionThrown() throws Exception {
        long id = 408L;
        PrincipalProfile profile = makePrincipalProfile(id);
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
        assertThat(error.getErrorMessage()).isEqualTo("Profile with ID:" + id + " is not exists.");
    }

    @Test
    @Transactional
    void shouldNotDeletePrincipalProfile_GeneralCannotDeleteExceptionThrown() throws Exception {
        Long id = 409L;
        PrincipalProfile profile = makePrincipalProfile(id);
        String errorMessage = "Cannot delete principal profile: '409'";
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
    @Transactional
    void shouldDeletePrincipalProfileById() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
        long id = profile.getId();
        assertThat(facade.findPrincipalProfileById(id)).isPresent();
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
        assertThat(facade.findPrincipalProfileById(id)).isEmpty();
    }

    @Test
    @Transactional
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
    @Transactional
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
        verify(facade, never()).deleteById(anyLong());
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '410!'");
    }

    @Test
    @Transactional
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
        verify(facade, never()).deleteById(anyLong());
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-410'");
    }

    @Test
    @Transactional
    void shouldNotDeletePrincipalProfileById_ExceptionThrown() throws Exception {
        long id = 411L;
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

    // private methods
    private PrincipalProfile getPersistent(PrincipalProfile newInstance) {
        Optional<PrincipalProfile> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
package oleg.sopilnyak.test.endpoint.end2end.rest.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.profile.PrincipalProfileRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.factory.Mappers;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        PrincipalProfileRestController.class, AspectForRestConfiguration.class,
        BusinessLogicConfiguration.class, PersistenceConfiguration.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
class PrincipalProfileRestControllerTest extends MysqlTestModelFactory {
    private static final String PROFILE_PRINCIPAL_FIND_BY_ID = "school::person::profile::principal:find.By.Id";
    private static final String PROFILE_PRINCIPAL_CREATE_OR_UPDATE = "school::person::profile::principal:create.Or.Update";
    private static final String ROOT = "/profiles/principals";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);

    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @Autowired
    CommandsFactory<PrincipalProfileCommand<?>> factory;
    @MockitoSpyBean
    @Autowired
    PrincipalProfileFacade facade;
    @MockitoSpyBean
    @Autowired
    AdviseDelegate delegate;
    @MockitoSpyBean
    @Autowired
    PrincipalProfileRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        deleteEntities(PrincipalProfileEntity.class);
    }

    @Test
    void everythingShouldBeValid() {
        assertThat(factory).isNotNull();
        assertThat(mapper).isNotNull();

        assertThat(facade).isNotNull();
        assertThat(factory).isEqualTo(ReflectionTestUtils.getField(facade, "factory"));

        assertThat(controller).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(facade).isEqualTo(ReflectionTestUtils.getField(controller, "facade"));
    }

    @Test
    @WithMockUser(authorities = {"PROF_GET"})
    void shouldFindPrincipalProfile() throws Exception {
        var profile = getPersistent(makePrincipalProfile(null));
        Long id = profile.getId();
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        // check the results
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertProfilesEquals(profile, dto);
        // check the behavior
        verify(controller).findById(String.valueOf(id));
        verify(facade).doActionAndResult(PROFILE_PRINCIPAL_FIND_BY_ID, id);
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"PROF_GET"})
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

        // check the results
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -402 is not found");
        // check the behavior
        verify(controller).findById(Long.toString(id));
        verify(facade).doActionAndResult(PROFILE_PRINCIPAL_FIND_BY_ID, id);
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"PROF_GET"})
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
        verify(facade, never()).doActionAndResult(anyString(), anyLong());
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '-402!'");
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"PROF_UPDATE"})
    void shouldUpdatePrincipalProfile() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
        String originalEmail = profile.getEmail();
        profile.setEmail("principal.profile@email.com");
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
        verify(facade).doActionAndResult(eq(PROFILE_PRINCIPAL_CREATE_OR_UPDATE), any(PrincipalProfile.class));
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), PrincipalProfileDto.class);
        assertProfilesEquals(profile, dto);
        assertThat(originalEmail).isNotEqualTo(dto.getEmail());
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"PROF_UPDATE"})
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
        verify(facade, never()).doActionAndResult(anyString(), any(PrincipalProfile.class));
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: 'null'");
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"PROF_UPDATE"})
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
        verify(facade, never()).doActionAndResult(anyString(), any(PrincipalProfile.class));
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong principal profile-id: '" + profile.getId() + "'");
        checkControllerAspect();
    }

    @Test
    @WithMockUser(authorities = {"PROF_UPDATE"})
    void shouldNotUpdatePrincipalProfile_ExceptionThrown() throws Exception {
        PrincipalProfilePayload profile = mapper.toPayload(getPersistent(makePrincipalProfile(null)));
        String jsonContent = MAPPER.writeValueAsString(MAPPER_DTO.toDto(profile));
        String message = "Cannot update principal profile: '406!'";
        Exception exception = new RuntimeException(message);
        doThrow(exception).when(facade).doActionAndResult(eq(PROFILE_PRINCIPAL_CREATE_OR_UPDATE), any(PrincipalProfile.class));

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
        verify(facade).doActionAndResult(eq(PROFILE_PRINCIPAL_CREATE_OR_UPDATE), any(PrincipalProfile.class));
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(error.getErrorMessage()).isEqualTo(message);
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(PrincipalProfileRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(PrincipalProfileRestController.class);
    }

    private PrincipalProfile getPersistent(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }
}
package oleg.sopilnyak.test.endpoint.end2end.rest.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.configuration.ActionContextReleaseInterceptor;
import oleg.sopilnyak.test.endpoint.dto.profile.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.profile.StudentProfileRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
class StudentProfileRestControllerTest extends MysqlTestModelFactory {
    private static final String ROOT = "/profiles/students";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EndpointMapper MAPPER_DTO = Mappers.getMapper(EndpointMapper.class);
    @Autowired
    PersistenceFacade database;
    @SpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @Autowired
    CommandsFactory<StudentProfileCommand> factory;
    @SpyBean
    @Autowired
    StudentProfileFacade facade;

    StudentProfileRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = spy(new StudentProfileRestController(facade));
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
    void shouldFindStudentProfile() throws Exception {
        var profile = getPersistent(makeStudentProfile(null));
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
        verify(facade).findStudentProfileById(id);
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertProfilesEquals(profile, dto);
    }

    @Test
    @Transactional
    void shouldNotFoundStudentProfile_NegativeId() throws Exception {
        Long id = -401L;
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
        verify(facade).findStudentProfileById(id);
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with id: -401 is not found");
    }

    @Test
    @Transactional
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
        verify(facade, never()).findStudentProfileById(anyLong());
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '401!'");
    }

    @Test
    @Transactional
    void shouldUpdateStudentProfile() throws Exception {
        StudentProfilePayload profile = mapper.toPayload(getPersistent(makeStudentProfile(null)));
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

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade).createOrUpdateProfile(any(StudentProfile.class));
        var dto = MAPPER.readValue(result.getResponse().getContentAsString(), StudentProfileDto.class);
        assertProfilesEquals(dto, profile);
        assertThat(originalEmail).isNotEqualTo(dto.getEmail());
    }

    @Test
    @Transactional
    void shouldNotUpdateStudentProfile_NullId() throws Exception {
        StudentProfilePayload profile = mapper.toPayload(getPersistent(makeStudentProfile(null)));
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

        verify(controller).update(any(StudentProfileDto.class));
        verify(facade, never()).createOrUpdateProfile(any(StudentProfile.class));
        var error = MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: 'null'");
    }

    @Test
    @Transactional
    void shouldNotUpdateStudentProfile_NegativeId() throws Exception {
        StudentProfilePayload profile = mapper.toPayload(getPersistent(makeStudentProfile(null)));
        long id = profile.getId();
        profile.setId(-id);
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
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student profile-id: '-" + id + "'");
    }

    @Test
    @Transactional
    void shouldNotUpdateStudentProfile_ExceptionThrown() throws Exception {
        StudentProfilePayload profile = mapper.toPayload(getPersistent(makeStudentProfile(null)));
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

    // private methods
    private StudentProfile getPersistent(StudentProfile newInstance) {
        Optional<StudentProfile> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
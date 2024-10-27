package oleg.sopilnyak.test.endpoint.end2end.rest.organization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.organization.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.organization.AuthorityPersonsRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.AuthorityPersonEntity;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {BusinessLogicConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class AuthorityPersonsRestControllerTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/authorities";

    @Autowired
    PersistenceFacade database;
    @Autowired
    CommandsFactory<AuthorityPersonCommand> factory;
    @SpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @Autowired
    AuthorityPersonFacade facade;
    AuthorityPersonsRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = spy(new AuthorityPersonsRestController(facade));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldLoginAuthorityPerson() throws Exception {
        String username = "test-username";
        String password = "test-password";
        AuthorityPerson person = create(makeCleanAuthorityPerson(212));
        assertThat(database.updateAuthorityPersonAccess(person, username, password)).isTrue();
        if (person instanceof AuthorityPersonEntity entity) {
            entity.setFaculties(List.of());
            database.save(entity);
        }
        String requestPath = ROOT + "/login";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .param("username", username)
                                        .param("password", password)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).login(username, password);
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto, false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotLoginAuthorityPerson_WrongLoginUsername() throws Exception {
        String username = "test-username";
        String password = "test-password";
        AuthorityPerson person = create(makeCleanAuthorityPerson(213));
        assertThat(database.updateAuthorityPersonAccess(person, username, password)).isTrue();
        if (person instanceof AuthorityPersonEntity entity) {
            entity.setFaculties(List.of());
            database.save(entity);
        }
        String requestPath = ROOT + "/login";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .param("username", "username")
                                        .param("password", password)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).login("username", password);
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Profile with login:'username', is not found");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotLoginAuthorityPerson_WrongPassword() throws Exception {
        String username = "test-username";
        String password = "test-password";
        AuthorityPerson person = create(makeCleanAuthorityPerson(214));
        assertThat(database.updateAuthorityPersonAccess(person, username, password)).isTrue();
        if (person instanceof AuthorityPersonEntity entity) {
            entity.setFaculties(List.of());
            database.save(entity);
        }
        String requestPath = ROOT + "/login";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .param("username", username)
                                        .param("password", "password")
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isForbidden())
                        .andDo(print())
                        .andReturn();

        verify(controller).login(username, "password");
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(403);
        assertThat(error.getErrorMessage()).isEqualTo("Login authority person command failed for username:" + username);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllAuthorities() throws Exception {
        int personsAmount = 30;
        List<AuthorityPerson> staff = IntStream.range(0, personsAmount)
                .mapToObj(i -> getPersistent(makeCleanAuthorityPerson(i + 1)))
                .sorted(Comparator.comparing(AuthorityPerson::getFullName)) // controller is ordering so
                .collect(Collectors.toList());

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(ROOT)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAll();
        String responseString = result.getResponse().getContentAsString();
        var authorityPersonList = MAPPER.readValue(responseString, new TypeReference<List<AuthorityPersonDto>>() {
        }).stream().map(course -> (AuthorityPerson) course).toList();

        assertThat(authorityPersonList).hasSize(personsAmount);
        assertAuthorityPersonLists(staff, authorityPersonList);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAuthorityPersonById() throws Exception {
        AuthorityPerson person = getPersistent(makeCleanAuthorityPerson(100));
        Long id = person.getId();
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
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateAuthorityPerson() throws Exception {
        AuthorityPerson person = makeCleanAuthorityPerson(200);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).createPerson(any(AuthorityPersonDto.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto, false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateAuthorityPerson() throws Exception {
        AuthorityPerson person = getPersistent(makeCleanAuthorityPerson(201));
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUpdateAuthorityPerson_WrongId_Negative() throws Exception {
        Long id = -301L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '-301'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUpdateAuthorityPerson_WrongId_Null() throws Exception {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: 'null'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteAuthorityPerson() throws Exception {
        AuthorityPerson person = create(makeCleanAuthorityPerson(202));
        if (person instanceof AuthorityPersonEntity entity) {
            entity.setFaculties(List.of());
            database.save(entity);
        }
        Long id = person.getId();
        assertThat(database.findAuthorityPersonById(id)).isPresent();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deletePerson(id.toString());
        assertThat(database.findAuthorityPersonById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPerson_WrongId_Null() throws Exception {
        String requestPath = ROOT + "/" + null;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson("null");
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: 'null'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPerson_WrongId_Negative() throws Exception {
        long id = -303L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(Long.toString(id));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '-303'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPerson_NotExists() throws Exception {
        long id = 304L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(Long.toString(id));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '304'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPerson_PersonAssignedToFaculty() throws Exception {
        AuthorityPerson source = makeCleanAuthorityPerson(203);
        if (source instanceof FakeAuthorityPerson fake) {
            fake.setFaculties(List.of(makeCleanFacultyNoDean(1)));
        }
        AuthorityPerson person = getPersistent(source);
        Long id = person.getId();
        assertThat(database.findAuthorityPersonById(id)).isPresent();
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(id.toString());
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error =
                MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("AuthorityPerson with ID:" + id + " is managing faculties.");
    }

    // private methods
    private AuthorityPerson getPersistent(AuthorityPerson newInstance) {
        Optional<AuthorityPerson> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private AuthorityPerson create(AuthorityPerson newInstance) {
        Optional<AuthorityPerson> saved = facade.create(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
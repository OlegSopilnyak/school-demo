package oleg.sopilnyak.test.endpoint.end2end.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.rest.AuthorityPersonsRestController;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.AuthorityPersonEntity;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
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
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static String ROOT = RequestMappingRoot.AUTHORITIES;

    @Autowired
    PersistenceFacade database;
    @Autowired
    OrganizationFacade facade;
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
        List<AuthorityPerson> authorityPersonDtos = MAPPER.readValue(responseString, new TypeReference<List<AuthorityPersonDto>>() {
        }).stream().map(course -> (AuthorityPerson) course).toList();

        assertThat(authorityPersonDtos).hasSize(personsAmount);
        assertAuthorityPersonLists(staff, authorityPersonDtos);
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
                        .andExpect(status().isOk())
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: 'null'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteAuthorityPerson() throws Exception {
        AuthorityPerson person = getPersistent(makeCleanAuthorityPerson(202));
        if (person instanceof AuthorityPersonEntity ape) {
            ape.setFaculties(List.of());
            database.save(ape);
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: 'null'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPerson_WrongId_Negative() throws Exception {
        Long id = -303L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(id.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '-303'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPerson_NotExists() throws Exception {
        Long id = 304L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(id.toString());
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(
                result.getResponse().getContentAsString(),
                RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '304'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPerson_PersonAssignedToFaculty() throws Exception {
        AuthorityPerson person = getPersistent(makeCleanAuthorityPerson(203));
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
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Cannot delete authority person for id = " + id);
    }

    private AuthorityPerson getPersistent(AuthorityPerson newInstance) {
        Optional<AuthorityPerson> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
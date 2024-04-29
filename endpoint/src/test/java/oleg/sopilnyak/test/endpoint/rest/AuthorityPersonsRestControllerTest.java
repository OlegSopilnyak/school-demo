package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class AuthorityPersonsRestControllerTest extends TestModelFactory {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    AuthorityPersonFacade facade;
    @Spy
    @InjectMocks
    AuthorityPersonsRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }


    @Test
    void shouldFindAllAuthorities() throws Exception {
        int personsAmount = 10;
        Collection<AuthorityPerson> staff = makeAuthorityPersons(personsAmount);
        when(facade.findAllAuthorityPersons()).thenReturn(staff);
        String requestPath = RequestMappingRoot.AUTHORITIES;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAll();
        verify(facade).findAllAuthorityPersons();
        String responseString = result.getResponse().getContentAsString();

        List<AuthorityPerson> authorityPersonDtos = MAPPER.readValue(responseString, new TypeReference<List<AuthorityPersonDto>>() {
        }).stream().map(course -> (AuthorityPerson) course).toList();

        assertThat(authorityPersonDtos).hasSize(personsAmount);
        assertAuthorityPersonLists(staff.stream().toList(), authorityPersonDtos);
    }

    @Test
    void shouldFindAuthorityPersonById() throws Exception {
        Long id = 300L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        when(facade.findAuthorityPersonById(id)).thenReturn(Optional.of(person));
        String requestPath = RequestMappingRoot.AUTHORITIES + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id.toString());
        verify(facade).findAuthorityPersonById(id);
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
    }

    @Test
    void shouldCreateAuthorityPerson() throws Exception {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        String requestPath = RequestMappingRoot.AUTHORITIES;
        doAnswer(invocation -> {
            AuthorityPerson received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertAuthorityPersonEquals(person, received);
            return Optional.of(person);
        }).when(facade).createOrUpdateAuthorityPerson(any(AuthorityPerson.class));
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).createPerson(any(AuthorityPersonDto.class));
        verify(facade).createOrUpdateAuthorityPerson(any(AuthorityPerson.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
    }

    @Test
    void shouldUpdateAuthorityPerson() throws Exception {
        Long id = 301L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        String requestPath = RequestMappingRoot.AUTHORITIES;
        doAnswer(invocation -> {
            AuthorityPerson received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertAuthorityPersonEquals(person, received);
            return Optional.of(person);
        }).when(facade).createOrUpdateAuthorityPerson(any(AuthorityPerson.class));
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).updatePerson(any(AuthorityPersonDto.class));
        verify(facade).createOrUpdateAuthorityPerson(any(AuthorityPerson.class));
        String responseString = result.getResponse().getContentAsString();
        AuthorityPerson personDto = MAPPER.readValue(responseString, AuthorityPersonDto.class);

        assertAuthorityPersonEquals(person, personDto);
    }

    @Test
    void shouldNotUpdateAuthorityPerson_WrongId_Negative() throws Exception {
        Long id = -301L;
        AuthorityPerson person = makeTestAuthorityPerson(id);
        String requestPath = RequestMappingRoot.AUTHORITIES;
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
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
    void shouldNotUpdateAuthorityPerson_WrongId_Null() throws Exception {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        String requestPath = RequestMappingRoot.AUTHORITIES;
        String jsonContent = MAPPER.writeValueAsString(person);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
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
    void shouldDeleteAuthorityPerson() throws Exception {
        Long id = 302L;
        String requestPath = RequestMappingRoot.AUTHORITIES + "/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deletePerson(id.toString());
        verify(facade).deleteAuthorityPersonById(id);
    }

    @Test
    void shouldNotDeleteAuthorityPerson_WrongId_Null() throws Exception {
        String requestPath = RequestMappingRoot.AUTHORITIES + "/" + null;
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
    void shouldNotDeleteAuthorityPerson_WrongId_Negative() throws Exception {
        Long id = -303L;
        String requestPath = RequestMappingRoot.AUTHORITIES + "/" + id;
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
    void shouldNotDeleteAuthorityPerson_NotExists() throws Exception {
        Long id = 304L;
        String requestPath = RequestMappingRoot.AUTHORITIES + "/" + id;
        doThrow(new NotExistAuthorityPersonException("Cannot delete not exists authority-person"))
                .when(facade).deleteAuthorityPersonById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(id.toString());
        verify(facade).deleteAuthorityPersonById(id);
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong authority-person-id: '304'");
    }

    @Test
    void shouldNotDeleteAuthorityPerson_PersonAssignedToFaculty() throws Exception {
        Long id = 305L;
        String requestPath = RequestMappingRoot.AUTHORITIES + "/" + id;
        doThrow(new AuthorityPersonManageFacultyException("Cannot delete not free authority-person"))
                .when(facade).deleteAuthorityPersonById(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deletePerson(id.toString());
        verify(facade).deleteAuthorityPersonById(id);
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Cannot delete authority person for id = 305");
    }
}
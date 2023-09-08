package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Course;
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
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class AuthorityPersonsRestControllerTest extends TestModelFactory {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    OrganizationFacade facade;
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
        when(facade.findAllAuthorityPerson()).thenReturn(staff);
        String requestPath = RequestMappingRoot.AUTHORITIES;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAllAuthorities();
        verify(facade).findAllAuthorityPerson();
        String responseString = result.getResponse().getContentAsString();

        List<AuthorityPerson> authorityPersonDtos = MAPPER.readValue(responseString, new TypeReference<List<AuthorityPersonDto>>() {
        }).stream().map(course -> (AuthorityPerson)course).toList();

        assertThat(authorityPersonDtos).hasSize(personsAmount);
        assertAuthorityPersonLists(staff.stream().toList(), authorityPersonDtos);
    }

    @Test
    void findById() {
    }

    @Test
    void createPerson() {
    }

    @Test
    void updatePerson() {
    }

    @Test
    void deleteCourse() {
    }
}
package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class FacultiesRestControllerTest extends TestModelFactory {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    OrganizationFacade facade;
    @Spy
    @InjectMocks
    FacultiesRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }


    @Test
    void shouldFindAllFaculties() throws Exception {
        int personsAmount = 10;
        Collection<Faculty> faculties = makeFaculties(personsAmount);
        when(facade.findAllFaculty()).thenReturn(faculties);
        String requestPath = RequestMappingRoot.FACULTIES;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAll();
        verify(facade).findAllFaculty();
        String responseString = result.getResponse().getContentAsString();

        List<Faculty> facultyDtos = MAPPER.readValue(responseString, new TypeReference<List<FacultyDto>>() {
        }).stream().map(course -> (Faculty) course).toList();

        assertThat(facultyDtos).hasSize(personsAmount);
        assertFacultyLists(faculties.stream().toList(), facultyDtos);
    }

    @Test
    void shouldFindFacultyById() throws Exception {
        Long id = 400L;
        Faculty faculty = makeTestFaculty(id);
        when(facade.getFacultyById(id)).thenReturn(Optional.of(faculty));
        String requestPath = RequestMappingRoot.FACULTIES + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findById(id.toString());
        verify(facade).getFacultyById(id);
        String responseString = result.getResponse().getContentAsString();
        Faculty facultyDto = MAPPER.readValue(responseString, FacultyDto.class);

        assertFacultyEquals(faculty, facultyDto);
    }

    @Test
    void create() {
    }

    @Test
    void update() {
    }

    @Test
    void delete() {
    }
}
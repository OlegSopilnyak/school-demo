package oleg.sopilnyak.test.endpoint.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
        when(facade.findAllFaculties()).thenReturn(faculties);
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
        verify(facade).findAllFaculties();
        String responseString = result.getResponse().getContentAsString();

        List<Faculty> facultyDtos = MAPPER.readValue(responseString, new TypeReference<List<FacultyDto>>() {
        }).stream().map(course -> (Faculty) course).toList();

        assertThat(facultyDtos).hasSize(personsAmount);
        assertFacultyLists(faculties.stream().toList(), facultyDtos, false);
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
    void shouldCreateFaculty() throws Exception {
        Long id = null;
        Faculty faculty = makeTestFaculty(id);
        doAnswer(invocation -> {
            Faculty received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertFacultyEquals(faculty, received);
            return Optional.of(faculty);
        }).when(facade).createOrUpdateFaculty(any(Faculty.class));
        String jsonContent = MAPPER.writeValueAsString(faculty);
        String requestPath = RequestMappingRoot.FACULTIES;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(FacultyDto.class));
        verify(facade).createOrUpdateFaculty(any(Faculty.class));
        String responseString = result.getResponse().getContentAsString();
        Faculty facultyDto = MAPPER.readValue(responseString, FacultyDto.class);

        assertFacultyEquals(faculty, facultyDto);
    }

    @Test
    void shouldUpdateFaculty() throws Exception {
        Long id = 402L;
        Faculty faculty = makeTestFaculty(id);
        doAnswer(invocation -> {
            Faculty received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertFacultyEquals(faculty, received);
            return Optional.of(faculty);
        }).when(facade).createOrUpdateFaculty(any(Faculty.class));
        String jsonContent = MAPPER.writeValueAsString(faculty);
        String requestPath = RequestMappingRoot.FACULTIES;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(FacultyDto.class));
        verify(facade).createOrUpdateFaculty(any(Faculty.class));
        String responseString = result.getResponse().getContentAsString();
        Faculty facultyDto = MAPPER.readValue(responseString, FacultyDto.class);

        assertFacultyEquals(faculty, facultyDto);
    }

    @Test
    void shouldNotUpdateFaculty_WrongId_Null() throws Exception {
        Faculty faculty = makeTestFaculty(null);
        String jsonContent = MAPPER.writeValueAsString(faculty);
        String requestPath = RequestMappingRoot.FACULTIES;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();


        verify(controller).update(any(FacultyDto.class));
        verify(facade, never()).createOrUpdateFaculty(any(Faculty.class));
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong faculty-id: 'null'").isEqualTo(error.getErrorMessage());
    }

    @Test
    void shouldNotUpdateFaculty_WrongId_Negative() throws Exception {
        Long id = -403L;
        Faculty faculty = makeTestFaculty(id);
        String jsonContent = MAPPER.writeValueAsString(faculty);
        String requestPath = RequestMappingRoot.FACULTIES;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(requestPath)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();


        verify(controller).update(any(FacultyDto.class));
        verify(facade, never()).createOrUpdateFaculty(any(Faculty.class));
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong faculty-id: '-403'").isEqualTo(error.getErrorMessage());
    }

    @Test
    void shouldDeleteFaculty() throws Exception {
        Long id = 410L;
        String requestPath = RequestMappingRoot.FACULTIES + "/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        verify(facade).deleteFacultyById(id);
    }

    @Test
    void shouldNotDeleteFaculty_WrongId_Null() throws Exception {
        Long id = null;
        String requestPath = RequestMappingRoot.FACULTIES + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete("null");
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong faculty-id: 'null'").isEqualTo(error.getErrorMessage());
    }

    @Test
    void shouldNotDeleteFaculty_WrongId_Negative() throws Exception {
        Long id = -411L;
        String requestPath = RequestMappingRoot.FACULTIES + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete(id.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong faculty-id: '-411'").isEqualTo(error.getErrorMessage());
    }
}
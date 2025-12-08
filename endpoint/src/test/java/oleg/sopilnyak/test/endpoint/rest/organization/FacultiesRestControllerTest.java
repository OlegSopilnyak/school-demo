package oleg.sopilnyak.test.endpoint.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {EndpointConfiguration.class, BusinessLogicConfiguration.class})
@DirtiesContext
class FacultiesRestControllerTest extends TestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/faculties";

    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    FacultyFacade facade;
    @MockitoSpyBean
    @Autowired
    FacultiesRestController controller;
    @MockitoSpyBean
    @Autowired
    AdviseDelegate delegate;

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
        doReturn(Set.copyOf(faculties)).when(persistenceFacade).findAllFaculties();

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(ROOT)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAll();
        verify(facade).findAllFaculties();

        List<Faculty> facultyList =
                MAPPER.readValue(result.getResponse().getContentAsString(),
                        new TypeReference<List<FacultyDto>>() {
                        }).stream().map(Faculty.class::cast).toList();

        assertThat(facultyList).hasSize(personsAmount);
        assertFacultyLists(faculties.stream().toList(), facultyList, false);
        checkControllerAspect();
    }

    @Test
    void shouldFindFacultyById() throws Exception {
        Long id = 400L;
        Faculty faculty = makeTestFaculty(id);
        doReturn(Optional.of(faculty)).when(persistenceFacade).findFacultyById(id);
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
        verify(facade).findFacultyById(id);

        Faculty facultyDto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, facultyDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotFindFacultyById() throws Exception {
        Long id = 400L;
        doReturn(Optional.empty()).when(persistenceFacade).findFacultyById(id);
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
        verify(facade).findFacultyById(id);

        ActionErrorMessage error =
                MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Faculty with id: 400 is not found");
        checkControllerAspect();
    }

    @Test
    void shouldCreateFaculty() throws Exception {
        Faculty faculty = makeTestFaculty(null);
        doAnswer(invocation -> {
            Faculty received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertFacultyEquals(faculty, received);
            return Optional.of(faculty);
        }).when(facade).createOrUpdateFaculty(any(Faculty.class));
        String jsonContent = MAPPER.writeValueAsString(faculty);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(FacultyDto.class));
        verify(facade).createOrUpdateFaculty(any(Faculty.class));

        Faculty facultyDto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, facultyDto);
        checkControllerAspect();
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

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(FacultyDto.class));
        verify(facade).createOrUpdateFaculty(any(Faculty.class));

        Faculty facultyDto = MAPPER.readValue(result.getResponse().getContentAsString(), FacultyDto.class);
        assertFacultyEquals(faculty, facultyDto);
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateFaculty_WrongId_Null() throws Exception {
        Faculty faculty = makeTestFaculty(null);
        String jsonContent = MAPPER.writeValueAsString(faculty);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();


        verify(controller).update(any(FacultyDto.class));
        verify(facade, never()).createOrUpdateFaculty(any(Faculty.class));

        ActionErrorMessage error =
                MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: 'null'");
        checkControllerAspect();
    }

    @Test
    void shouldNotUpdateFaculty_WrongId_Negative() throws Exception {
        Long id = -403L;
        Faculty faculty = makeTestFaculty(id);
        String jsonContent = MAPPER.writeValueAsString(faculty);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();


        verify(controller).update(any(FacultyDto.class));
        verify(facade, never()).createOrUpdateFaculty(any(Faculty.class));

        ActionErrorMessage error =
                MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: '-403'");
        checkControllerAspect();
    }

    @Test
    void shouldDeleteFaculty() throws Exception {
        Long id = 410L;
        doReturn(Optional.of(mock(Faculty.class))).when(persistenceFacade).findFacultyById(id);
        String requestPath = ROOT + "/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        verify(facade).deleteFacultyById(id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteFaculty_WrongId_Null() throws Exception {
        String requestPath = ROOT + "/null";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete("null");

        ActionErrorMessage error =
                MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: 'null'");
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteFaculty_WrongId_Negative() throws Exception {
        long id = -411L;
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete(Long.toString(id));

        ActionErrorMessage error =
                MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: '-411'");
        checkControllerAspect();
    }

    @Test
    void shouldDeleteFacultyInstance() throws Exception {
        Long id = 410L;
        Faculty faculty = makeTestFaculty(id);
        if (faculty instanceof FakeFaculty fake) {
            fake.setCourses(List.of());
        } else {
            fail("Wrong type of the %s", faculty.toString());
        }
        doReturn(Optional.of(faculty)).when(persistenceFacade).findFacultyById(id);
        String jsonContent = MAPPER.writeValueAsString(faculty);

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(ROOT)
                                .content(jsonContent)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(any(FacultyDto.class));
        verify(facade).deleteFaculty(any(FacultyDto.class));
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteFacultyInstance() throws Exception {
        Long id = 410L;
        Faculty faculty = makeTestFaculty(id);
        String jsonContent = MAPPER.writeValueAsString(faculty);
        String errorMessage = "Faculty '" + id + "' not exists.";
        doThrow(new FacultyNotFoundException(errorMessage)).when(facade).deleteFaculty(any(FacultyDto.class));

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete(any(FacultyDto.class));
        verify(facade).deleteFaculty(any(FacultyDto.class));

        ActionErrorMessage error =
                MAPPER.readValue(result.getResponse().getContentAsString(), ActionErrorMessage.class);
        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(FacultiesRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(FacultiesRestController.class);
    }
}

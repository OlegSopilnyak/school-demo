package oleg.sopilnyak.test.endpoint.end2end.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.rest.FacultiesRestController;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.FacultyEntity;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.Optional;

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
class FacultiesRestControllerTest extends MysqlTestModelFactory {
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static String ROOT = RequestMappingRoot.FACULTIES;

    @Autowired
    PersistenceFacade database;
    @Autowired
    OrganizationFacade facade;

    FacultiesRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = spy(new FacultiesRestController(facade));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllFaculties() throws Exception {
        int personsAmount = 10;
        List<Faculty> faculties = makeCleanFaculties(personsAmount).stream().map(this::getPersistent).toList();

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

        List<Faculty> facultyDtos = MAPPER.readValue(responseString, new TypeReference<List<FacultyDto>>() {
        }).stream().map(course -> (Faculty) course).toList();

        assertThat(facultyDtos).hasSize(personsAmount);
        assertFacultyLists(faculties, facultyDtos, true);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindFacultyById() throws Exception {
        Faculty faculty = getPersistent(makeCleanFaculty(0));
        Long id = faculty.getId();
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
        Faculty facultyDto = MAPPER.readValue(responseString, FacultyDto.class);

        assertFacultyEquals(faculty, facultyDto);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateFaculty() throws Exception {
        Faculty faculty = makeCleanFaculty(1);
        String jsonContent = MAPPER.writeValueAsString(faculty);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(FacultyDto.class));
        String responseString = result.getResponse().getContentAsString();
        Faculty facultyDto = MAPPER.readValue(responseString, FacultyDto.class);

        assertThat(facultyDto.getId()).isNotNull();
        assertFacultyEquals(faculty, facultyDto, false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateFaculty() throws Exception {
        Faculty faculty = getPersistent(makeCleanFaculty(2));
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
        String responseString = result.getResponse().getContentAsString();
        Faculty facultyDto = MAPPER.readValue(responseString, FacultyDto.class);

        assertFacultyEquals(faculty, facultyDto);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: 'null'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: '-403'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteFaculty() throws Exception {
        Faculty faculty = getPersistent(makeCleanFaculty(2));
        if (faculty instanceof FacultyEntity fe) {
            fe.setCourses(List.of());
            database.save(fe);
        }
        Long id = faculty.getId();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        assertThat(database.findFacultyById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteFaculty_WrongId_Null() throws Exception {
        Long id = null;
        String requestPath = ROOT + "/" + id;

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

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: 'null'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteFaculty_WrongId_Negative() throws Exception {
        Long id = -411L;
        String requestPath = ROOT + "/" + id;

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

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong faculty-id: '-411'");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteFaculty_NotEmptyFaculty() throws Exception {
        Faculty faculty = getPersistent(makeCleanFaculty(2));
        Long id = faculty.getId();
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete(id.toString());
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(HttpStatus.CONFLICT.value()).isEqualTo(error.getErrorCode());
        assertThat("Cannot delete faculty for id = " + id).isEqualTo(error.getErrorMessage());
    }

    private Faculty getPersistent(Faculty newInstance) {
        Optional<Faculty> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
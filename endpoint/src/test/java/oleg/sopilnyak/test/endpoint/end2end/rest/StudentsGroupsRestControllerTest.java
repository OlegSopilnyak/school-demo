package oleg.sopilnyak.test.endpoint.end2end.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.endpoint.rest.StudentsGroupsRestController;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.StudentsGroupEntity;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
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

import java.util.List;
import java.util.Optional;
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
class StudentsGroupsRestControllerTest extends MysqlTestModelFactory {
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static String ROOT = RequestMappingRoot.STUDENT_GROUPS;

    @Autowired
    PersistenceFacade database;
    @Autowired
    OrganizationFacade facade;

    StudentsGroupsRestController controller;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = spy(new StudentsGroupsRestController(facade));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllStudentsGroups() throws Exception {
        int groupsAmount = 5;
        List<StudentsGroup> studentsGroups = IntStream.range(0, groupsAmount)
                .mapToObj(i -> getPersistent(makeCleanStudentsGroup(i + 1))).toList();

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
        List<StudentsGroup> studentsGroupDtos = MAPPER.readValue(responseString, new TypeReference<List<StudentsGroupDto>>() {
        }).stream().map(course -> (StudentsGroup) course).toList();

        assertThat(studentsGroupDtos).hasSize(groupsAmount);
        assertStudentsGroupLists(studentsGroups, studentsGroupDtos);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentsGroupById() throws Exception {
        StudentsGroup studentsGroup = getPersistent(makeCleanStudentsGroup(0));
        Long id = studentsGroup.getId();
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
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateStudentsGroup() throws Exception {
        StudentsGroup studentsGroup = makeCleanStudentsGroup(1);
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(StudentsGroupDto.class));
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto, false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateStudentsGroup() throws Exception {
        StudentsGroup studentsGroup = getPersistent(makeCleanStudentsGroup(2));
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentsGroupDto.class));
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUpdateStudentsGroup_WrongId_Null() throws Exception {
        Long id = null;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentsGroupDto.class));
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong students-group-id: 'null'").isEqualTo(error.getErrorMessage());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUpdateStudentsGroup_WrongId_Negative() throws Exception {
        Long id = -502L;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        String jsonContent = MAPPER.writeValueAsString(studentsGroup);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).update(any(StudentsGroupDto.class));
        String responseString = result.getResponse().getContentAsString();
        RestResponseEntityExceptionHandler.RestErrorMessage error = MAPPER.readValue(responseString, RestResponseEntityExceptionHandler.RestErrorMessage.class);

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong students-group-id: '-502'").isEqualTo(error.getErrorMessage());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudentsGroup() throws Exception {
        StudentsGroup studentsGroup = getPersistent(makeCleanStudentsGroup(2));
        if (studentsGroup instanceof StudentsGroupEntity sge) {
            sge.setStudents(List.of());
            database.save(sge);
        }
        Long id = studentsGroup.getId();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        assertThat(database.findStudentsGroupById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudentsGroup_WrongId_Null() throws Exception {
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

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong students-group-id: 'null'").isEqualTo(error.getErrorMessage());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudentsGroup_WrongId_Negative() throws Exception {
        Long id = -511L;
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

        assertThat(404).isEqualTo(error.getErrorCode());
        assertThat("Wrong students-group-id: '-511'").isEqualTo(error.getErrorMessage());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudentsGroup_GroupHasStudents() throws Exception {
        StudentsGroup studentsGroup = getPersistent(makeCleanStudentsGroup(2));
        Long id = studentsGroup.getId();
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

        assertThat(409).isEqualTo(error.getErrorCode());
        assertThat("Cannot delete students group for id = " + id).isEqualTo(error.getErrorMessage());
    }

    private StudentsGroup getPersistent(StudentsGroup newInstance) {
        Optional<StudentsGroup> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
package oleg.sopilnyak.test.endpoint.end2end.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.endpoint.rest.organization.StudentsGroupsRestController;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AspectForRestConfiguration.class, BusinessLogicConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class StudentsGroupsRestControllerTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/student-groups";

    @Autowired
    PersistenceFacade database;
    @Autowired
    CommandsFactory<StudentsGroupCommand<?>> factory;
    @SpyBean
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    StudentsGroupFacade facade;
    @SpyBean
    @Autowired
    AdviseDelegate delegate;
    @SpyBean
    @Autowired
    StudentsGroupsRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    @Transactional
    void everythingShouldBeValid() {
        assertThat(factory).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(database).isNotNull();

        assertThat(facade).isNotNull();
        assertThat(factory).isEqualTo(ReflectionTestUtils.getField(facade, "factory"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(facade, "mapper"));

        assertThat(controller).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(facade).isEqualTo(ReflectionTestUtils.getField(controller, "facade"));
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
        var groupList = MAPPER.readValue(responseString, new TypeReference<List<StudentsGroupDto>>() {
        }).stream().map(StudentsGroup.class::cast).toList();

        assertThat(groupList).hasSize(groupsAmount);
        assertStudentsGroupLists(studentsGroups, groupList);
        checkControllerAspect();
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
        var dto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, dto);
        checkControllerAspect();
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
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).create(any(StudentsGroupDto.class));
        String responseString = result.getResponse().getContentAsString();
        var dto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, dto, false);
        checkControllerAspect();
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
        var dto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, dto);
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUpdateStudentsGroup_WrongId_Null() throws Exception {
        StudentsGroup studentsGroup = makeTestStudentsGroup(null);
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
        var error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: 'null'");
        checkControllerAspect();
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
        var error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: '-502'");
        checkControllerAspect();
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
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudentsGroup_WrongId_Null() throws Exception {
        String requestPath = ROOT + "/" + null;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete("null");
        String responseString = result.getResponse().getContentAsString();
        var error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: 'null'");
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudentsGroup_WrongId_Negative() throws Exception {
        long id = -511L;
        String requestPath = ROOT + "/" + id;
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete(String.valueOf(id));
        String responseString = result.getResponse().getContentAsString();
        var error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: '-511'");
        checkControllerAspect();
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
        var error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo("Students Group with ID:" + id + " has students.");
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsGroupsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsGroupsRestController.class);
    }

    private StudentsGroup getPersistent(StudentsGroup newInstance) {
        Optional<StudentsGroup> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}

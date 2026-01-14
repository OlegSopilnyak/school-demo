package oleg.sopilnyak.test.endpoint.rest.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
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
class StudentsGroupsRestControllerTest extends TestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/student-groups";
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    StudentsGroupFacade facade;
    @MockitoSpyBean
    @Autowired
    StudentsGroupsRestController controller;
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
    void shouldFindAllStudentsGroups() throws Exception {
        int groupsAmount = 5;
        Collection<StudentsGroup> studentsGroups = makeStudentsGroups(groupsAmount);
        doReturn(Set.copyOf(studentsGroups)).when(persistenceFacade).findAllStudentsGroups();

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(ROOT)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findAll();
        verify(facade).findAllStudentsGroups();
        String responseString = result.getResponse().getContentAsString();

        List<StudentsGroup> studentsGroupList = MAPPER.readValue(responseString, new TypeReference<List<StudentsGroupDto>>() {
        }).stream().map(StudentsGroup.class::cast).toList();

        assertThat(studentsGroupList).hasSameSizeAs(studentsGroups).hasSize(groupsAmount);
        assertStudentsGroupLists(studentsGroups.stream().toList(), studentsGroupList);
        checkControllerAspect();
    }

    @Test
    void shouldFindStudentsGroupById() throws Exception {
        Long id = 500L;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        doReturn(Optional.of(studentsGroup)).when(persistenceFacade).findStudentsGroupById(id);
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
        verify(facade).findStudentsGroupById(id);
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
        checkControllerAspect();
    }

    @Test
    void shouldCreateStudentsGroup() throws Exception {
        StudentsGroup studentsGroup = makeTestStudentsGroup(null);
        doAnswer(invocation -> {
            StudentsGroup received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertStudentsGroupEquals(studentsGroup, received);
            return Optional.of(studentsGroup);
        }).when(facade).createOrUpdateStudentsGroup(any(StudentsGroup.class));
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
        verify(facade).createOrUpdateStudentsGroup(any(StudentsGroup.class));
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
        checkControllerAspect();
    }

    @Test
    void shouldUpdateStudentsGroup() throws Exception {
        Long id = 501L;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        doAnswer(invocation -> {
            StudentsGroup received = invocation.getArgument(0);
            assertThat(received.getId()).isEqualTo(id);
            assertStudentsGroupEquals(studentsGroup, received);
            return Optional.of(studentsGroup);
        }).when(facade).createOrUpdateStudentsGroup(any(StudentsGroup.class));
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
        verify(facade).createOrUpdateStudentsGroup(any(StudentsGroup.class));
        String responseString = result.getResponse().getContentAsString();
        StudentsGroup studentsGroupDto = MAPPER.readValue(responseString, StudentsGroupDto.class);

        assertStudentsGroupEquals(studentsGroup, studentsGroupDto);
        checkControllerAspect();
    }

    @Test
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
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: 'null'");
        checkControllerAspect();
    }

    @Test
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
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: '-502'");
        checkControllerAspect();
    }

    @Test
    void shouldDeleteStudentsGroup() throws Exception {
        Long id = 510L;
        doReturn(Optional.of(mock(StudentsGroup.class))).when(persistenceFacade).findStudentsGroupById(id);
        String requestPath = ROOT + "/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        verify(facade).deleteStudentsGroupById(id);
        checkControllerAspect();
    }

    @Test
    void shouldNotDeleteStudentsGroup_WrongId_Null() throws Exception {
        String requestPath = ROOT + "/null";
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).delete("null");
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: 'null'");
        checkControllerAspect();
    }

    @Test
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

        verify(controller).delete(Long.toString(id));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong students-group-id: '-511'");
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
}

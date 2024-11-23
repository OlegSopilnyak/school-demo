package oleg.sopilnyak.test.endpoint.rest.organization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
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
class StudentsGroupsRestControllerTest extends TestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/student-groups";

    @Mock
    StudentsGroupFacade facade;
    @Spy
    @InjectMocks
    StudentsGroupsRestController controller;
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
        when(facade.findAllStudentsGroups()).thenReturn(studentsGroups);

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
        }).stream().map(course -> (StudentsGroup) course).toList();

        assertThat(studentsGroupList).hasSize(groupsAmount);
        assertStudentsGroupLists(studentsGroups.stream().toList(), studentsGroupList);
    }

    @Test
    void shouldFindStudentsGroupById() throws Exception {
        Long id = 500L;
        StudentsGroup studentsGroup = makeTestStudentsGroup(id);
        when(facade.findStudentsGroupById(id)).thenReturn(Optional.of(studentsGroup));
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
    }

    @Test
    void shouldDeleteStudentsGroup() throws Exception {
        Long id = 510L;
        String requestPath = ROOT + "/" + id;
        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).delete(id.toString());
        verify(facade).deleteStudentsGroupById(id);
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
    }
}
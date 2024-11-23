package oleg.sopilnyak.test.endpoint.rest.education;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Student;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
class StudentsRestControllerTest extends TestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/students";

    @Mock
    StudentsFacade facade;
    @Spy
    @InjectMocks
    StudentsRestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @Test
    void shouldFindStudent() throws Exception {
        Long id = 100L;
        Student student = makeTestStudent(id);
        when(facade.findById(id)).thenReturn(Optional.of(student));
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findStudent(id.toString());
        String responseString = result.getResponse().getContentAsString();
        StudentDto studentDto = MAPPER.readValue(responseString, StudentDto.class);

        assertThat(id).isEqualTo(studentDto.getId());
        assertThat(student.getFirstName()).isEqualTo(studentDto.getFirstName());
        assertThat(student.getLastName()).isEqualTo(studentDto.getLastName());
        assertThat(student.getGender()).isEqualTo(studentDto.getGender());
        assertThat(student.getDescription()).isEqualTo(studentDto.getDescription());
        assertCourseLists(student.getCourses(), studentDto.getCourses());
    }

    @Test
    void shouldFindStudentsEnrolledForCourse() throws Exception {
        Long courseId = 200L;
        long studentsAmount = 40L;
        Set<Student> students = LongStream.range(0, studentsAmount).mapToObj(this::makeTestStudent).collect(Collectors.toSet());
        when(facade.findEnrolledTo(courseId)).thenReturn(students);
        String requestPath = ROOT + "/enrolled/" + courseId;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findEnrolledTo(courseId.toString());
        String responseString = result.getResponse().getContentAsString();
        List<Student> studentList =
                MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
                }).stream().map(student -> (Student) student).toList();

        assertThat(studentList).hasSize((int) studentsAmount);
        assertStudentLists(students.stream().toList(), studentList);
    }

    @Test
    void shouldFindStudentsWithEmptyCourses() throws Exception {
        long studentsAmount = 5L;
        Set<Student> students = LongStream.range(0, studentsAmount).mapToObj(this::makeTestStudent).collect(Collectors.toSet());
        when(facade.findNotEnrolled()).thenReturn(students);
        String requestPath = ROOT + "/empty";

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(requestPath)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).findNotEnrolledStudents();
        String responseString = result.getResponse().getContentAsString();
        List<Student> studentList =
                MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
                }).stream().map(student -> (Student) student).toList();

        assertThat(studentList).hasSize((int) studentsAmount);
        assertStudentLists(students.stream().toList(), studentList);
    }

    @Test
    void shouldCreateStudent() throws Exception {
        Student student = makeTestStudent(null);
        doAnswer(invocation -> {
            Student received = invocation.getArgument(0);
            assertThat(received.getId()).isNull();
            assertStudentEquals(student, received);
            assertCourseLists(student.getCourses(), received.getCourses());
            return Optional.of(student);
        }).when(facade).create(any(Student.class));
        String jsonContent = MAPPER.writeValueAsString(student);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.post(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn();

        verify(controller).createStudent(any(StudentDto.class));
        String responseString = result.getResponse().getContentAsString();
        StudentDto studentDto = MAPPER.readValue(responseString, StudentDto.class);

        assertThat(student.getLastName()).isEqualTo(studentDto.getLastName());
        assertThat(student.getGender()).isEqualTo(studentDto.getGender());
        assertThat(student.getDescription()).isEqualTo(studentDto.getDescription());
        assertCourseLists(student.getCourses(), studentDto.getCourses());
    }

    @Test
    void shouldUpdateValidStudent() throws Exception {
        Long id = 101L;
        Student student = makeTestStudent(id);
        doAnswer(invocation -> {
            Student received = invocation.getArgument(0);
            assertThat(id).isEqualTo(student.getId());
            assertStudentEquals(student, received);
            assertCourseLists(student.getCourses(), received.getCourses());
            return Optional.of(student);
        }).when(facade).createOrUpdate(any(Student.class));
        String jsonContent = MAPPER.writeValueAsString(student);


        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateStudent(any(StudentDto.class));
        String responseString = result.getResponse().getContentAsString();
        StudentDto studentDto = MAPPER.readValue(responseString, StudentDto.class);

        assertThat(id).isEqualTo(studentDto.getId());
        assertThat(student.getLastName()).isEqualTo(studentDto.getLastName());
        assertThat(student.getGender()).isEqualTo(studentDto.getGender());
        assertThat(student.getDescription()).isEqualTo(studentDto.getDescription());
        assertCourseLists(student.getCourses(), studentDto.getCourses());
    }

    @Test
    void shouldNotUpdateInvalidStudent_NullId() throws Exception {
        Student student = makeTestStudent(null);
        String jsonContent = MAPPER.writeValueAsString(student);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateStudent(any(StudentDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student-id: 'null'");
    }

    @Test
    void shouldNotUpdateInvalidStudent_NegativeId() throws Exception {
        Long id = -1001L;
        Student student = makeTestStudent(id);
        String jsonContent = MAPPER.writeValueAsString(student);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.put(ROOT)
                                        .content(jsonContent)
                                        .contentType(APPLICATION_JSON)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).updateStudent(any(StudentDto.class));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student-id: '-1001'");
    }

    @Test
    void shouldDeleteStudentValidId() throws Exception {
        long id = 102L;
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteStudent(Long.toString(id));
    }

    @Test
    void shouldNotDeleteStudentValidId_StudentNotExistsException() throws Exception {
        Long id = 103L;
        String requestPath = ROOT + "/" + id;
        doThrow(new StudentNotFoundException("Wrong student")).when(facade).delete(id);

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudent(id.toString());
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student-id: '103'");
    }

    @Test
    void shouldNotDeleteStudentValidId_StudentWithCoursesException() throws Exception {
        Long id = 104L;
        String requestPath = ROOT + "/" + id;
        String errorMessage = "Not empty courses set";
        doThrow(new StudentWithCoursesException(errorMessage)).when(facade).delete(id);
        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isConflict())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudent(id.toString());
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(409);
        assertThat(error.getErrorMessage()).isEqualTo(errorMessage);
    }
}
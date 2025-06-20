package oleg.sopilnyak.test.endpoint.end2end.rest.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import oleg.sopilnyak.test.endpoint.aspect.AspectDelegate;
import oleg.sopilnyak.test.endpoint.configuration.AspectForRestConfiguration;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.rest.education.StudentsRestController;
import oleg.sopilnyak.test.endpoint.rest.exceptions.ActionErrorMessage;
import oleg.sopilnyak.test.endpoint.rest.exceptions.RestResponseEntityExceptionHandler;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AspectForRestConfiguration.class, BusinessLogicConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class StudentsRestControllerTest extends MysqlTestModelFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT = "/students";

    @Autowired
    PersistenceFacade database;
    @Autowired
    CommandsFactory<StudentCommand<?>> factory;
    @SpyBean
    @Autowired
    BusinessMessagePayloadMapper mapper;
    @Autowired
    StudentsFacade facade;
    @SpyBean
    @Autowired
    StudentsRestController controller;
    @SpyBean
    @Autowired
    AspectDelegate delegate;

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
        assertThat(mapper).isNotNull();
        assertThat(database).isNotNull();

        assertThat(facade).isNotNull();
        assertThat(factory).isEqualTo(ReflectionTestUtils.getField(facade, "factory"));
        assertThat(mapper).isEqualTo(ReflectionTestUtils.getField(facade, "mapper"));

        assertThat(controller).isNotNull();
        assertThat(delegate).isNotNull();
        assertThat(facade).isEqualTo(ReflectionTestUtils.getField(controller, "facade"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudent() throws Exception {
        Student student = getPersistent(makeClearTestStudent());
        Long id = student.getId();
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

        assertStudentEquals(student, studentDto);
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentsEnrolledForCourse() throws Exception {
        Course course = getPersistent(makeClearCourse(0));
        Long courseId = course.getId();
        int studentsAmount = 40;
        IntStream.range(0, studentsAmount).forEach(i -> {
            if (course instanceof CourseEntity ce) {
                ce.add(getPersistent(makeClearStudent(i)));
            }
        });

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
        List<Student> studentList = MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
        }).stream().map(Student.class::cast).toList();

        assertThat(studentList).hasSize(studentsAmount);
        assertStudentLists(database.findCourseById(courseId).orElseThrow().getStudents(), studentList);
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentsWithEmptyCourses() throws Exception {
        int studentsAmount = 5;
        Set<Student> students = IntStream.range(0, studentsAmount)
                .mapToObj(i -> getPersistent(makeClearStudent(i))).collect(Collectors.toSet());
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
        var studentList = MAPPER.readValue(responseString, new TypeReference<List<StudentDto>>() {
        }).stream().map(Student.class::cast).toList();

        assertThat(studentList).hasSize(studentsAmount);
        assertStudentLists(students.stream().sorted(Comparator.comparing(Student::getFullName)).toList(), studentList);
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateStudent() throws Exception {
        Student student = makeClearTestStudent();
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

        assertStudentEquals(student, studentDto, false);
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateValidStudent() throws Exception {
        Student student = getPersistent(makeClearStudent(0));
        Long studentId = student.getId();
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

        assertStudentEquals(database.findStudentById(studentId).orElseThrow(), studentDto, true);
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudentValidId() throws Exception {
        Student student = createStudent(makeClearStudent(0));
        Long id = student.getId();
        assertThat(database.findStudentById(id)).isPresent();
        String requestPath = ROOT + "/" + id;

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(requestPath)
                )
                .andExpect(status().isOk())
                .andDo(print());

        verify(controller).deleteStudent(id.toString());
        assertThat(database.findStudentById(id)).isEmpty();
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudent_StudentNotExistsException() throws Exception {
        long id = 103L;
        String requestPath = ROOT + "/" + id;

        MvcResult result =
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(requestPath)
                        )
                        .andExpect(status().isNotFound())
                        .andDo(print())
                        .andReturn();

        verify(controller).deleteStudent(String.valueOf(id));
        String responseString = result.getResponse().getContentAsString();
        ActionErrorMessage error = MAPPER.readValue(responseString, ActionErrorMessage.class);

        assertThat(error.getErrorCode()).isEqualTo(404);
        assertThat(error.getErrorMessage()).isEqualTo("Wrong student-id: '103'");
        checkControllerAspect();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudent_StudentWithCoursesException() throws Exception {
        Student student = createStudent(makeClearStudent(0));
        Long id = student.getId();
        assertThat(database.findStudentById(id)).isPresent();
        String requestPath = ROOT + "/" + id;
        if (student instanceof StudentPayload payload && payload.getOriginal() instanceof StudentEntity entity) {
            entity.add(getPersistent(makeClearCourse(0)));
        }

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
        assertThat(error.getErrorMessage()).isEqualTo("Student with ID:" + id + " has registered courses.");
        checkControllerAspect();
    }

    // private methods
    private void checkControllerAspect() {
        final ArgumentCaptor<JoinPoint> aspectCapture = ArgumentCaptor.forClass(JoinPoint.class);
        verify(delegate).beforeCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsRestController.class);
        verify(delegate).afterCall(aspectCapture.capture());
        assertThat(aspectCapture.getValue().getTarget()).isInstanceOf(StudentsRestController.class);
    }

    private Course getPersistent(Course newInstance) {
        Optional<Course> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private Student createStudent(Student newInstance) {
        Optional<Student> saved = facade.create(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private Student getPersistent(Student newInstance) {
        Optional<Student> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}
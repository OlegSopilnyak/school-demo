package oleg.sopilnyak.test.end2end.facade.student;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.education.StudentIsNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.student.*;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.facade.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class StudentsFacadeImplTest extends MysqlTestModelFactory {
    private static final String STUDENT_FIND_BY_ID = "student.findById";
    private static final String STUDENT_FIND_ENROLLED_TO = "student.findEnrolledTo";
    private static final String STUDENT_FIND_NOT_ENROLLED = "student.findNotEnrolled";
    private static final String STUDENT_CREATE_OR_UPDATE = "student.createOrUpdate";
    private static final String STUDENT_CREATE_NEW = "student.create.macro";
    private static final String STUDENT_DELETE = "student.delete.macro";

    @Autowired
    PersistenceFacade database;

    PersistenceFacade persistenceFacade;
    CommandsFactory<StudentCommand> factory;

    StudentsFacadeImpl facade;
    BusinessMessagePayloadMapper payloadMapper;

    CreateOrUpdateStudentCommand createStudentCommand;
    CreateOrUpdateStudentProfileCommand createProfileCommand;
    DeleteStudentCommand deleteStudentCommand;
    DeleteStudentProfileCommand deleteProfileCommand;
    DeleteStudentMacroCommand deleteStudentMacroCommand;

    @BeforeEach
    void setUp() {
        payloadMapper = spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        createStudentCommand = spy(new CreateOrUpdateStudentCommand(persistenceFacade, payloadMapper));
        createProfileCommand = spy(new CreateOrUpdateStudentProfileCommand(persistenceFacade, payloadMapper));
        deleteStudentCommand = spy(new DeleteStudentCommand(persistenceFacade, payloadMapper));
        deleteProfileCommand = spy(new DeleteStudentProfileCommand(persistenceFacade, payloadMapper));
        deleteStudentMacroCommand = spy(new DeleteStudentMacroCommand(deleteStudentCommand, deleteProfileCommand, persistenceFacade, 10));
        deleteStudentMacroCommand.runThreadPoolExecutor();
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new StudentsFacadeImpl(factory, payloadMapper));
    }

    @AfterEach
    void tearDown() {
        deleteStudentMacroCommand.stopThreadPoolExecutor();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void allComponentsShouldBeValid() {
        assertThat(database).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistenceFacade).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindById() {
        Long studentId = 100L;

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isEmpty();
        verify(factory).command(STUDENT_FIND_BY_ID);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(studentId);
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindById() {
        Student newStudent = makeClearTestStudent();
        Long studentId = getPersistentStudent(newStudent).getId();

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isNotEmpty();
        assertStudentEquals(newStudent, student.get(), false);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(studentId);
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindEnrolledTo() {
        Student newStudent = makeClearTestStudent();
        Student saved = getPersistentStudent(newStudent);
        Long courseId = saved.getCourses().get(0).getId();

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).hasSize(1);
        assertStudentEquals(newStudent, students.iterator().next(), false);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(courseId);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindEnrolledTo_NoCourseById() {
        Long courseId = 200L;

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(courseId);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindEnrolledTo_NoEnrolledStudents() {
        Course course = makeClearCourse(0);
        Long courseId = getPersistentCourse(course).getId();

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertCourseEquals(course, database.findCourseById(courseId).orElse(null), false);
        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(courseId);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindNotEnrolled() {
        Student newStudent = makeClearTestStudent();
        if (newStudent instanceof FakeStudent student) {
            student.setCourses(List.of());
        }
        getPersistentStudent(newStudent);

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).hasSize(1);
        assertStudentEquals(newStudent, students.iterator().next(), false);
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(null);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindNotEnrolled_StudentNotExists() {

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(null);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindNotEnrolled_StudentHasCourses() {
        getPersistentStudent(makeClearTestStudent());

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(null);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdate_Create() {
        Student student = makeClearTestStudent();

        Optional<Student> result = facade.create(student);

        assertStudentEquals(student, result.orElseThrow(), false);
        verify(factory).command(STUDENT_CREATE_NEW);
        verify(factory.command(STUDENT_CREATE_NEW)).createContext(any(StudentPayload.class));
        verify(factory.command(STUDENT_CREATE_NEW)).doCommand(any(Context.class));
        verify(persistenceFacade).save(any(StudentPayload.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdate_Update() {
        Student student = getPersistentStudent(makeClearTestStudent());
        Student oldStudent = payloadMapper.toPayload(student);
        if (oldStudent instanceof StudentPayload entity) {
            entity.setFirstName(student.getFirstName() + "-newOne");
        }
        reset(persistenceFacade);

        Optional<Student> result = facade.createOrUpdate(oldStudent);

        assertThat(result).isNotEmpty();
        assertStudentEquals(oldStudent, result.orElseThrow());
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(oldStudent);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(student.getId());
        verify(persistenceFacade).save(oldStudent);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDelete() throws StudentWithCoursesException, StudentIsNotFoundException {
        Student newStudent = makeClearTestStudent();
        if (newStudent instanceof FakeStudent student) {
            student.setCourses(List.of());
        }
        StudentPayload created = createStudent(newStudent);
        Long studentId = created.getId();
        Long profileId = created.getProfileId();
        assertThat(database.findStudentById(studentId)).isPresent();
        assertThat(database.findStudentProfileById(profileId)).isPresent();

        facade.delete(studentId);

        assertThat(database.findStudentById(studentId)).isEmpty();
        assertThat(database.findStudentProfileById(profileId)).isEmpty();
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).deleteStudent(studentId);
        verify(persistenceFacade).deleteProfileById(profileId);
        // 1. building context for delete
        // 2. deleting student
        verify(persistenceFacade, times(2)).findStudentById(studentId);
        verify(persistenceFacade).findStudentProfileById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_StudentNotExists() {
        Long studentId = 101L;

        StudentIsNotFoundException exception = assertThrows(StudentIsNotFoundException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:101 is not exists.");
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).deleteStudent(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_StudentWithCourses() {
        Long studentId = getPersistentStudent(makeClearTestStudent()).getId();

        StudentWithCoursesException exception = assertThrows(StudentWithCoursesException.class, () -> facade.delete(studentId));

        assertThat("Student with ID:" + studentId + " has registered courses.").isEqualTo(exception.getMessage());
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        // 1. building context for delete
        // 2. deleting student
        verify(persistenceFacade, times(2)).findStudentById(studentId);
        verify(persistenceFacade, never()).deleteStudent(anyLong());
    }

    // private methods
    private StudentPayload createStudent(Student newStudent) {
        return (StudentPayload) facade.create(newStudent).orElseThrow();
    }

    private Student getPersistentStudent(Student newStudent) {
        Optional<Student> saved = database.save(newStudent);
        assertThat(saved).isPresent();
        return saved.get();
    }

    private Course getPersistentCourse(Course newCourse) {
        Optional<Course> saved = database.save(newCourse);
        assertThat(saved).isPresent();
        return saved.get();
    }

    private CommandsFactory<StudentCommand> buildFactory(PersistenceFacade persistenceFacade) {
        return spy(new StudentCommandsFactory(
                        Set.of(
                                spy(new FindStudentCommand(persistenceFacade)),
                                spy(new FindEnrolledStudentsCommand(persistenceFacade)),
                                spy(new FindNotEnrolledStudentsCommand(persistenceFacade)),
                                createStudentCommand,
                                spy(new CreateStudentMacroCommand(createStudentCommand, createProfileCommand, payloadMapper)),
                                deleteStudentCommand,
                                deleteStudentMacroCommand
                        )
                )
        );
    }
}
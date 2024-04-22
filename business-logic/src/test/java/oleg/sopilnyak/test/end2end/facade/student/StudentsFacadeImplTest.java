package oleg.sopilnyak.test.end2end.facade.student;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.student.*;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.facade.impl.StudentsFacadeImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
    private static final String STUDENT_DELETE = "student.delete";


    @SpyBean
    @Autowired
    StudentRepository studentRepository;
    @Autowired
    PersistenceFacade database;

    PersistenceFacade persistenceFacade;
    CommandsFactory<?> factory;

    StudentsFacadeImpl<?> facade;

    @BeforeEach
    void setUp() {
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new StudentsFacadeImpl<>(factory));
    }

    @AfterEach
    void tearDown() {
        reset(studentRepository);
    }

    @Test
    void allComponentsShouldBeValid() {
        assertThat(studentRepository).isNotNull();
        assertThat(database).isNotNull();
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
        verify(studentRepository).findById(studentId);
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
        verify(studentRepository).findById(studentId);
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
        verify(studentRepository).findStudentEntitiesByCourseSetId(courseId);
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
        verify(studentRepository).findStudentEntitiesByCourseSetId(courseId);
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
        verify(studentRepository).findStudentEntitiesByCourseSetId(courseId);
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
        verify(studentRepository).findStudentEntitiesByCourseSetEmpty();
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
        verify(studentRepository).findStudentEntitiesByCourseSetEmpty();
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
        verify(studentRepository).findStudentEntitiesByCourseSetEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdate_Create() {
        Student student = makeClearTestStudent();

        Optional<Student> result = facade.createOrUpdate(student);

        assertThat(result).isNotEmpty();
        assertStudentEquals(student, result.get(), false);
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(student);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(student);
        verify(studentRepository).saveAndFlush(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdate_Update() {
        Student student = getPersistentStudent(makeClearTestStudent());
        Student oldStudent = persistenceFacade.toEntity(student);
        if (oldStudent instanceof StudentEntity entity) {
            entity.setFirstName(student.getFirstName() + "-newOne");
        }
        reset(persistenceFacade, studentRepository);

        Optional<Student> result = facade.createOrUpdate(oldStudent);

        assertThat(result).isNotEmpty();
        assertStudentEquals(oldStudent, result.get());
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(student);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(student.getId());
        verify(studentRepository).findById(student.getId());
        verify(persistenceFacade).save(oldStudent);
        verify(studentRepository).saveAndFlush(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDelete() throws StudentWithCoursesException, NotExistStudentException {
        Student newStudent = makeClearTestStudent();
        if (newStudent instanceof FakeStudent student) {
            student.setCourses(List.of());
        }
        Long studentId = getPersistentStudent(newStudent).getId();
        assertThat(database.findStudentById(studentId)).isPresent();
        reset(studentRepository);

        facade.delete(studentId);

        assertThat(database.findStudentById(studentId)).isEmpty();
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).deleteStudent(studentId);
        verify(persistenceFacade).findStudentById(studentId);
        verify(studentRepository, times(2)).findById(studentId);
        verify(studentRepository).deleteById(studentId);
        verify(studentRepository).flush();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_StudentNotExists() {
        Long studentId = 101L;

        NotExistStudentException exception = assertThrows(NotExistStudentException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:101 is not exists.");
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(studentRepository).findById(studentId);
        verify(persistenceFacade, never()).deleteStudent(studentId);
        verify(studentRepository, never()).deleteById(studentId);
        verify(studentRepository, never()).flush();
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
        verify(persistenceFacade).findStudentById(studentId);
        verify(studentRepository).findById(studentId);
        verify(persistenceFacade, never()).deleteStudent(studentId);
        verify(studentRepository, never()).deleteById(studentId);
        verify(studentRepository, never()).flush();
    }

    // private methods
    private Student getPersistentStudent(Student newStudent) {
        Optional<Student> saved = database.save(newStudent);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private Course getPersistentCourse(Course newCourse) {
        Optional<Course> saved = database.save(newCourse);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private CommandsFactory<?> buildFactory(PersistenceFacade persistenceFacade) {
        return new StudentCommandsFactory(
                Set.of(
                        spy(new FindStudentCommand(persistenceFacade)),
                        spy(new FindEnrolledStudentsCommand(persistenceFacade)),
                        spy(new FindNotEnrolledStudentsCommand(persistenceFacade)),
                        spy(new CreateOrUpdateStudentCommand(persistenceFacade)),
                        spy(new DeleteStudentCommand(persistenceFacade))
                )
        );
    }
}
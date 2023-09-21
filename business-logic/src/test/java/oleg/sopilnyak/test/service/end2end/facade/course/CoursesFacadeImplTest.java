package oleg.sopilnyak.test.service.end2end.facade.course;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.course.*;
import oleg.sopilnyak.test.service.end2end.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.service.facade.course.CourseCommandsFacade;
import oleg.sopilnyak.test.service.facade.course.CoursesFacadeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CoursesFacadeImplTest extends MysqlTestModelFactory {
    @Autowired
    PersistenceFacade database;
    PersistenceFacade persistenceFacade;
    CommandsFactory factory;

    CoursesFacadeImpl facade;

    @BeforeEach
    void setUp() {
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new CoursesFacadeImpl(factory));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindById() {
        Long courseId = 100L;

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isEmpty();
        verify(factory).command(CourseCommandsFacade.FIND_BY_ID);
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindById() {
        Course newCourse = makeClearTestCourse();
        Long courseId = getPersistentCourse(newCourse).getId();


        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.get(), false);
        verify(factory).command(CourseCommandsFacade.FIND_BY_ID);
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindRegisteredFor() {
        Long studentId = 200L;

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).isEmpty();
        verify(factory).command(CourseCommandsFacade.FIND_REGISTERED);
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindRegisteredFor() {
        Course newCourse = makeClearTestCourse();
        Course savedCourse = getPersistentCourse(newCourse);
        Long studentId = savedCourse.getStudents().get(0).getId();

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.iterator().next(), false);
        verify(factory).command(CourseCommandsFacade.FIND_REGISTERED);
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindWithoutStudents() {

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).isEmpty();
        verify(factory).command(CourseCommandsFacade.FIND_NOT_REGISTERED);
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindWithoutStudents() {
        Course newCourse = makeClearCourse(0);
        getPersistentCourse(newCourse);

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.iterator().next(), false);
        verify(factory).command(CourseCommandsFacade.FIND_NOT_REGISTERED);
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdate() {
        Course courseToUpdate = mock(Course.class);

        Optional<Course> course = facade.createOrUpdate(courseToUpdate);

        assertThat(course).isNotEmpty();
        assertCourseEquals(courseToUpdate, course.get(), false);
        verify(factory).command(CourseCommandsFacade.CREATE_OR_UPDATE);
        verify(persistenceFacade).save(courseToUpdate);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDelete() throws CourseNotExistsException, CourseWithStudentsException {
        Course newCourse = makeClearCourse(0);
        Long courseId = getPersistentCourse(newCourse).getId();

        facade.delete(courseId);

        assertThat(database.findCourseById(courseId)).isEmpty();
        verify(factory).command(CourseCommandsFacade.DELETE);
        verify(persistenceFacade).deleteCourse(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_CourseNotExists() {
        Long courseId = 101L;

        CourseNotExistsException exception = assertThrows(CourseNotExistsException.class, () -> facade.delete(courseId));

        assertThat("Course with ID:101 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.DELETE);
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_CourseWithStudents() {
        Course newCourse = makeClearTestCourse();
        Long courseId = getPersistentCourse(newCourse).getId();

        CourseWithStudentsException exception = assertThrows(CourseWithStudentsException.class, () -> facade.delete(courseId));

        assertThat("Course with ID:" + courseId + " has enrolled students.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.DELETE);
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegister() throws CourseNotExistsException, NoRoomInTheCourseException, StudentCoursesExceedException, StudentNotExistsException {
        Student student = makeClearStudent(0);
        Long studentId = getPersistentStudent(student).getId();
        Course course = makeClearCourse(0);
        Long courseId = getPersistentCourse(course).getId();

        facade.register(studentId, courseId);

        Optional<Course> courseEntity = database.findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents().get(0).getId()).isEqualTo(studentId);
        Optional<Student> studentEntity = database.findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses().get(0).getId()).isEqualTo(courseId);

        verify(factory).command(CourseCommandsFacade.REGISTER);
        verify(persistenceFacade).link(studentEntity.get(), courseEntity.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegister_AlreadyLinked() throws CourseNotExistsException, NoRoomInTheCourseException, StudentCoursesExceedException, StudentNotExistsException {
        Student student = getPersistentStudent(makeClearStudent(0));
        Long studentId = student.getId();
        Course course = getPersistentCourse(makeClearCourse(0));
        Long courseId = course.getId();
        database.link(student, course);
        Optional<Course> courseEntity = database.findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents().get(0).getId()).isEqualTo(studentId);
        Optional<Student> studentEntity = database.findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses().get(0).getId()).isEqualTo(courseId);

        facade.register(studentId, courseId);


        verify(factory).command(CourseCommandsFacade.REGISTER);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_StudentNotExists() {
        Long studentId = 202L;
        Long courseId = 102L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.register(studentId, courseId));

        assertThat("Student with ID:202 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.REGISTER);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_NoRoomInTheCourse() {
        Student student = makeClearStudent(0);
        Long studentId = getPersistentStudent(student).getId();
        Course course = makeClearTestCourse();
        Long courseId = getPersistentCourse(course).getId();

        NoRoomInTheCourseException exception = assertThrows(NoRoomInTheCourseException.class, () -> facade.register(studentId, courseId));

        assertThat("Course with ID:" + courseId + " does not have enough rooms.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.REGISTER);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_StudentCoursesExceed() {
        Student student = makeClearTestStudent();
        Long studentId = getPersistentStudent(student).getId();
        Course course = makeClearCourse(0);
        Long courseId = getPersistentCourse(course).getId();


        StudentCoursesExceedException exception = assertThrows(StudentCoursesExceedException.class, () -> facade.register(studentId, courseId));

        assertThat("Student with ID:" + studentId + " exceeds maximum courses.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.REGISTER);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_CourseNotExists() {
        Student student = makeClearStudent(0);
        Long studentId = getPersistentStudent(student).getId();
        Long courseId = 102L;

        CourseNotExistsException exception = assertThrows(CourseNotExistsException.class, () -> facade.register(studentId, courseId));

        assertThat("Course with ID:102 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.REGISTER);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnRegister_NotLinked() throws CourseNotExistsException, StudentNotExistsException {
        Long studentId = getPersistentStudent(makeClearStudent(0)).getId();
        Long courseId = getPersistentCourse(makeClearCourse(0)).getId();

        facade.unRegister(studentId, courseId);

        Optional<Course> courseEntity = database.findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents()).isEmpty();
        Optional<Student> studentEntity = database.findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses()).isEmpty();
        verify(factory).command(CourseCommandsFacade.UN_REGISTER);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnRegister_Linked() throws CourseNotExistsException, StudentNotExistsException {
        Student student = getPersistentStudent(makeClearStudent(0));
        Long studentId = student.getId();
        Course course = getPersistentCourse(makeClearCourse(0));
        Long courseId = course.getId();
        database.link(student, course);
        Optional<Course> courseEntity = database.findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents().get(0).getId()).isEqualTo(studentId);
        Optional<Student> studentEntity = database.findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses().get(0).getId()).isEqualTo(courseId);

        facade.unRegister(studentId, courseId);

        courseEntity = database.findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents()).isEmpty();
        studentEntity = database.findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses()).isEmpty();
        verify(factory).command(CourseCommandsFacade.UN_REGISTER);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUnRegister_StudentNotExists() {
        Long studentId = 203L;
        Long courseId = 103L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.unRegister(studentId, courseId));

        assertThat("Student with ID:203 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.UN_REGISTER);
        verify(persistenceFacade, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUnRegister_CourseNotExists() {
        Long studentId = getPersistentStudent(makeClearStudent(0)).getId();
        Long courseId = 103L;

        Exception exception = assertThrows(CourseNotExistsException.class, () -> facade.unRegister(studentId, courseId));

        assertThat("Course with ID:103 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.UN_REGISTER);
        verify(persistenceFacade, never()).unLink(any(Student.class), any(Course.class));
    }

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

    private CommandsFactory buildFactory(PersistenceFacade persistenceFacade) {
        return new SchoolCommandsFactory(
                Set.of(
                        new FindCourseCommand(persistenceFacade),
                        new FindRegisteredCoursesCommand(persistenceFacade),
                        new FindCoursesWithoutStudentsCommand(persistenceFacade),
                        new CreateOrUpdateCourseCommand(persistenceFacade),
                        new DeleteCourseCommand(persistenceFacade),
                        new RegisterStudentToCourseCommand(persistenceFacade, 2, 2),
                        new UnRegisterStudentFromCourseCommand(persistenceFacade)
                )
        );
    }

}
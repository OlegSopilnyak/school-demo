package oleg.sopilnyak.test.end2end.facade.course;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.executable.course.*;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.facade.impl.CoursesFacadeImpl;
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
//@ExtendWith(SpringExtension.class)
//@ContextConfiguration(classes = {CourseCommandsConfiguration.class, CourseFacadeConfiguration.class})
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "school.hibernate.hbm2ddl.auto=update"
})
@Rollback
class CoursesFacadeIntegrationTest extends MysqlTestModelFactory {
    public static final String COURSE_FIND_BY_ID = "course.findById";
    public static final String COURSE_FIND_REGISTERED_FOR = "course.findRegisteredFor";
    public static final String COURSE_FIND_WITHOUT_STUDENTS = "course.findWithoutStudents";
    public static final String COURSE_CREATE_OR_UPDATE = "course.createOrUpdate";
    public static final String COURSE_DELETE = "course.delete";
    public static final String COURSE_REGISTER = "course.register";
    public static final String COURSE_UN_REGISTER = "course.unRegister";

    @Autowired
    @SpyBean
    PersistenceFacade database;
    PersistenceFacade persistenceFacade;
    CommandsFactory factory;

    CoursesFacade facade;

    @BeforeEach
    void setUp() {
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new CoursesFacadeImpl(factory));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindById() {
        String commandId = COURSE_FIND_BY_ID;
        Long courseId = 100L;

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindById() {
        String commandId = COURSE_FIND_BY_ID;
        Course newCourse = makeClearTestCourse();
        Long courseId = getPersistent(newCourse).getId();


        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.orElse(null), false);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindRegisteredFor() {
        String commandId = COURSE_FIND_REGISTERED_FOR;
        Long studentId = 200L;

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindRegisteredFor() {
        String commandId = COURSE_FIND_REGISTERED_FOR;
        Course newCourse = makeClearTestCourse();
        Course savedCourse = getPersistent(newCourse);
        Long studentId = savedCourse.getStudents().get(0).getId();

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.iterator().next(), false);
        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindWithoutStudents() {
        String commandId = COURSE_FIND_WITHOUT_STUDENTS;

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindWithoutStudents() {
        String commandId = COURSE_FIND_WITHOUT_STUDENTS;
        Course newCourse = makeClearCourse(0);
        getPersistent(newCourse);

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.iterator().next(), false);
        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdate() {
        String commandId = COURSE_CREATE_OR_UPDATE;
        Course courseToUpdate = mock(Course.class);

        Optional<Course> course = facade.createOrUpdate(courseToUpdate);

        assertThat(course).isPresent();
        assertCourseEquals(courseToUpdate, course.get(), false);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseToUpdate);
        verify(persistenceFacade).save(courseToUpdate);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDelete() throws CourseNotExistsException, CourseWithStudentsException {
        String commandId = COURSE_DELETE;
        Course newCourse = makeClearCourse(0);
        Long courseId = getPersistent(newCourse).getId();
        assertThat(database.findCourseById(courseId)).isPresent();

        facade.delete(courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).deleteCourse(courseId);
        assertThat(database.findCourseById(courseId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_CourseNotExists() {
        String commandId = COURSE_DELETE;
        Long courseId = 101L;

        CourseNotExistsException exception = assertThrows(CourseNotExistsException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:101 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_CourseWithStudents() {
        String commandId = COURSE_DELETE;
        Course newCourse = makeClearTestCourse();
        Long courseId = getPersistent(newCourse).getId();
        assertThat(database.findCourseById(courseId)).isPresent();

        CourseWithStudentsException exception = assertThrows(CourseWithStudentsException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:" + courseId + " has enrolled students.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegister() throws CourseNotExistsException, NoRoomInTheCourseException, StudentCoursesExceedException, StudentNotExistsException {
        String commandId = COURSE_REGISTER;
        Student student = makeClearStudent(0);
        Long studentId = getPersistent(student).getId();
        Course course = makeClearCourse(0);
        Long courseId = getPersistent(course).getId();

        facade.register(studentId, courseId);

        Optional<Course> courseEntity = database.findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents().get(0).getId()).isEqualTo(studentId);
        Optional<Student> studentEntity = database.findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses().get(0).getId()).isEqualTo(courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).link(studentEntity.orElse(null), courseEntity.orElse(null));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegister_AlreadyLinked() throws CourseNotExistsException, NoRoomInTheCourseException, StudentCoursesExceedException, StudentNotExistsException {
        String commandId = COURSE_REGISTER;
        Student student = getPersistent(makeClearStudent(0));
        Long studentId = student.getId();
        Course course = getPersistent(makeClearCourse(0));
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


        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_StudentNotExists() {
        String commandId = COURSE_REGISTER;
        Long studentId = 202L;
        Long courseId = 102L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:202 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(any());
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_NoRoomInTheCourse() {
        String commandId = COURSE_REGISTER;
        Student student = makeClearStudent(0);
        Long studentId = getPersistent(student).getId();
        Course course = makeClearTestCourse();
        Long courseId = getPersistent(course).getId();

        NoRoomInTheCourseException exception = assertThrows(NoRoomInTheCourseException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:" + courseId + " does not have enough rooms.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_StudentCoursesExceed() {
        String commandId = COURSE_REGISTER;
        Student student = makeClearTestStudent();
        Long studentId = getPersistent(student).getId();
        Course course = makeClearCourse(0);
        Long courseId = getPersistent(course).getId();


        StudentCoursesExceedException exception = assertThrows(StudentCoursesExceedException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:" + studentId + " exceeds maximum courses.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_CourseNotExists() {
        String commandId = COURSE_REGISTER;
        Student student = makeClearStudent(0);
        Long studentId = getPersistent(student).getId();
        Long courseId = 102L;

        CourseNotExistsException exception = assertThrows(CourseNotExistsException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:102 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnRegister_NotLinked() throws CourseNotExistsException, StudentNotExistsException {
        String commandId = COURSE_UN_REGISTER;
        Long studentId = getPersistent(makeClearStudent(0)).getId();
        Long courseId = getPersistent(makeClearCourse(0)).getId();

        facade.unRegister(studentId, courseId);

        Optional<Course> courseEntity = database.findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents()).isEmpty();
        Optional<Student> studentEntity = database.findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses()).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnRegister_Linked() throws CourseNotExistsException, StudentNotExistsException {
        String commandId = COURSE_UN_REGISTER;
        Student student = getPersistent(makeClearStudent(0));
        Long studentId = student.getId();
        Course course = getPersistent(makeClearCourse(0));
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
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUnRegister_StudentNotExists() {
        String commandId = COURSE_UN_REGISTER;
        Long studentId = 203L;
        Long courseId = 103L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:203 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(any());
        verify(persistenceFacade, never()).unLink(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUnRegister_CourseNotExists() {
        String commandId = COURSE_UN_REGISTER;
        Long studentId = getPersistent(makeClearStudent(0)).getId();
        Long courseId = 103L;

        Exception exception = assertThrows(CourseNotExistsException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:103 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).unLink(any(), any());
    }

    private Student getPersistent(Student newInstance) {
        Optional<Student> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private Course getPersistent(Course newInstance) {
        Optional<Course> saved = database.save(newInstance);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private CommandsFactory buildFactory(PersistenceFacade persistenceFacade) {
        return new CourseCommandsFactory(List.of(
                spy(new FindCourseCommand(persistenceFacade)),
                spy(new FindRegisteredCoursesCommand(persistenceFacade)),
                spy(new FindCoursesWithoutStudentsCommand(persistenceFacade)),
                spy(new CreateOrUpdateCourseCommand(persistenceFacade)),
                spy(new DeleteCourseCommand(persistenceFacade)),
                spy(new RegisterStudentToCourseCommand(persistenceFacade, 50, 5)),
                spy(new UnRegisterStudentFromCourseCommand(persistenceFacade))
        ));
    }

}
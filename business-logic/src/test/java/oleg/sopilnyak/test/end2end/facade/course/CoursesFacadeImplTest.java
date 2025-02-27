package oleg.sopilnyak.test.end2end.facade.course;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.education.*;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.course.*;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.facade.impl.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
class CoursesFacadeImplTest extends MysqlTestModelFactory {
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
    CommandsFactory<CourseCommand<?>> factory;
    BusinessMessagePayloadMapper payloadMapper;

    CoursesFacade facade;

    @BeforeEach
    void setUp() {
        payloadMapper = spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new CoursesFacadeImpl(factory, payloadMapper));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(database).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindById() {
        Long courseId = 100L;

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isEmpty();
        verify(factory).command(COURSE_FIND_BY_ID);
        verify(factory.command(COURSE_FIND_BY_ID)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindById() {
        Course newCourse = makeClearTestCourse();
        Long courseId = getPersistent(newCourse).getId();


        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.orElseThrow(), false);
        verify(factory).command(COURSE_FIND_BY_ID);
        verify(factory.command(COURSE_FIND_BY_ID)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindRegisteredFor() {
        Long studentId = 200L;

        Set<Course> courses = facade.findRegisteredFor(studentId);

        assertThat(courses).isEmpty();
        verify(factory).command(COURSE_FIND_REGISTERED_FOR);
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).createContext(Input.of(studentId));
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindRegisteredFor() {
        Course newCourse = makeClearTestCourse();
        Course savedCourse = getPersistent(newCourse);
        Long studentId = savedCourse.getStudents().get(0).getId();
        assertThat(studentId).isNotNull();

        Set<Course> courses = facade.findRegisteredFor(studentId);

        assertThat(courses).isNotEmpty();
        assertCourseEquals(newCourse, courses.iterator().next(), false);
        assertThat(courses).hasSize(1);
        verify(factory).command(COURSE_FIND_REGISTERED_FOR);
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).createContext(Input.of(studentId));
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindWithoutStudents() {

        Set<Course> courses = facade.findWithoutStudents();

        assertThat(courses).isEmpty();
        verify(factory).command(COURSE_FIND_WITHOUT_STUDENTS);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).createContext(null);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindWithoutStudents() {
        Course newCourse = makeClearCourse(0);
        getPersistent(newCourse);

        Set<Course> courses = facade.findWithoutStudents();

        assertThat(courses).isNotEmpty();
        assertCourseEquals(newCourse, courses.iterator().next(), false);
        assertThat(courses).hasSize(1);
        verify(factory).command(COURSE_FIND_WITHOUT_STUDENTS);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).createContext(null);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdate() {
        Course newCourse = makeClearCourse(1);
        Course courseToUpdate = getPersistent(newCourse);

        Optional<Course> course = facade.createOrUpdate(courseToUpdate);

        assertThat(course).isPresent();
        assertCourseEquals(courseToUpdate, course.get(), false);
        verify(factory).command(COURSE_CREATE_OR_UPDATE);
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).createContext(any(Input.class));
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDelete() throws CourseNotFoundException, CourseWithStudentsException {
        Course newCourse = makeClearCourse(0);
        Long courseId = getPersistent(newCourse).getId();
        assertThat(database.findCourseById(courseId)).isPresent();

        facade.delete(courseId);

        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).deleteCourse(courseId);
        assertThat(database.findCourseById(courseId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_CourseNotExists() {
        Long courseId = 101L;

        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:101 is not exists.");
        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete_CourseWithStudents() {
        Course newCourse = makeClearTestCourse();
        Long courseId = getPersistent(newCourse).getId();
        assertThat(database.findCourseById(courseId)).isPresent();

        CourseWithStudentsException exception = assertThrows(CourseWithStudentsException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:" + courseId + " has enrolled students.");
        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegister() throws CourseNotFoundException, CourseHasNoRoomException, StudentCoursesExceedException, StudentNotFoundException {
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

        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).link(studentEntity.orElseThrow(), courseEntity.orElseThrow());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRegister_AlreadyLinked() throws CourseNotFoundException, CourseHasNoRoomException, StudentCoursesExceedException, StudentNotFoundException {
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


        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_StudentNotExists() {
        Long studentId = 202L;
        Long courseId = 102L;

        Exception exception = assertThrows(StudentNotFoundException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:202 is not exists.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_NoRoomInTheCourse() {
        Student student = makeClearStudent(0);
        Long studentId = getPersistent(student).getId();
        Course course = makeClearTestCourse();
        Long courseId = getPersistent(course).getId();

        CourseHasNoRoomException exception = assertThrows(CourseHasNoRoomException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:" + courseId + " does not have enough rooms.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_StudentCoursesExceed() {
        Student student = makeClearTestStudent();
        Long studentId = getPersistent(student).getId();
        Course course = makeClearCourse(0);
        Long courseId = getPersistent(course).getId();


        StudentCoursesExceedException exception = assertThrows(StudentCoursesExceedException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:" + studentId + " exceeds maximum courses.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotRegister_CourseNotExists() {
        Student student = makeClearStudent(0);
        Long studentId = getPersistent(student).getId();
        Long courseId = 102L;

        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:102 is not exists.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnRegister_NotLinked() throws CourseNotFoundException, StudentNotFoundException {
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
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnRegister_Linked() throws CourseNotFoundException, StudentNotFoundException {
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
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUnRegister_StudentNotExists() {
        Long studentId = 203L;
        Long courseId = 103L;

        Exception exception = assertThrows(StudentNotFoundException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:203 is not exists.");
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(any());
        verify(persistenceFacade, never()).unLink(any(), any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUnRegister_CourseNotExists() {
        Long studentId = getPersistent(makeClearStudent(0)).getId();
        Long courseId = 103L;

        Exception exception = assertThrows(CourseNotFoundException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:103 is not exists.");
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
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
        saved.orElseThrow().getStudents().forEach(student -> database.save(student));
        return saved.get();
    }

    private CommandsFactory<CourseCommand<?>> buildFactory(PersistenceFacade persistenceFacade) {
        return new CourseCommandsFactory(List.of(
                spy(new FindCourseCommand(persistenceFacade)),
                spy(new FindRegisteredCoursesCommand(persistenceFacade)),
                spy(new FindCoursesWithoutStudentsCommand(persistenceFacade)),
                spy(new CreateOrUpdateCourseCommand(persistenceFacade, payloadMapper)),
                spy(new DeleteCourseCommand(persistenceFacade, payloadMapper)),
                spy(new RegisterStudentToCourseCommand(persistenceFacade, payloadMapper, 50, 5)),
                spy(new UnRegisterStudentFromCourseCommand(persistenceFacade, payloadMapper))
        ));
    }
}
package oleg.sopilnyak.test.service.facade.course;

import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.executable.course.*;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursesFacadeUnitTest {
    private static final String COURSE_FIND_BY_ID = "course.findById";
    private static final String COURSE_FIND_REGISTERED_FOR = "course.findRegisteredFor";
    private static final String COURSE_FIND_WITHOUT_STUDENTS = "course.findWithoutStudents";
    private static final String COURSE_CREATE_OR_UPDATE = "course.createOrUpdate";
    private static final String COURSE_DELETE = "course.delete";
    private static final String COURSE_REGISTER = "course.register";
    private static final String COURSE_UN_REGISTER = "course.unRegister";
    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    @Spy
    CommandsFactory factory = buildFactory();

    @Spy
    @InjectMocks
    CoursesFacadeImpl facade;

    @Mock
    Course mockedCourse;
    @Mock
    Student mockedStudent;

    @Test
    void shouldNotFindById() {
        String commandId = COURSE_FIND_BY_ID;
        Long courseId = 200L;

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    void shouldFindById() {
        String commandId = COURSE_FIND_BY_ID;
        Long courseId = 201L;
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    void shouldFindRegisteredFor() {
        String commandId = COURSE_FIND_REGISTERED_FOR;
        Long studentId = 100L;
        when(persistenceFacade.findCoursesRegisteredForStudent(studentId)).thenReturn(Set.of(mockedCourse));

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    void shouldNotFindRegisteredFor() {
        String commandId = COURSE_FIND_REGISTERED_FOR;
        Long studentId = 101L;

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    void shouldFindWithoutStudents() {
        String commandId = COURSE_FIND_WITHOUT_STUDENTS;
        when(persistenceFacade.findCoursesWithoutStudents()).thenReturn(Set.of(mockedCourse));

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    void shouldNotFindWithoutStudents() {
        String commandId = COURSE_FIND_WITHOUT_STUDENTS;

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    void shouldCreateOrUpdate() {
        String commandId = COURSE_CREATE_OR_UPDATE;
        when(persistenceFacade.save(mockedCourse)).thenReturn(Optional.of(mockedCourse));

        Optional<Course> course = facade.createOrUpdate(mockedCourse);

        assertThat(course).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockedCourse);
        verify(persistenceFacade).save(mockedCourse);
    }

    @Test
    void shouldNotCreateOrUpdate() {
        String commandId = COURSE_CREATE_OR_UPDATE;

        Optional<Course> course = facade.createOrUpdate(mockedCourse);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockedCourse);
        verify(persistenceFacade).save(mockedCourse);
    }

    @Test
    void shouldDelete() throws CourseNotExistsException, CourseWithStudentsException {
        String commandId = COURSE_DELETE;
        Long courseId = 202L;
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        facade.delete(courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).deleteCourse(courseId);
    }

    @Test
    void shouldNotDelete_CourseNotExists() {
        String commandId = COURSE_DELETE;
        Long courseId = 203L;

        CourseNotExistsException exception = assertThrows(CourseNotExistsException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:203 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    void shouldNotDelete_CourseWithStudents() {
        String commandId = COURSE_DELETE;
        Long courseId = 204L;
        when(mockedCourse.getStudents()).thenReturn(List.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        CourseWithStudentsException exception = assertThrows(CourseWithStudentsException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:204 has enrolled students.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    void shouldRegister() throws
            CourseNotExistsException, NoRoomInTheCourseException,
            StudentCoursesExceedException, StudentNotExistsException {
        String commandId = COURSE_REGISTER;
        Long studentId = 102L;
        when(mockedStudent.getId()).thenReturn(studentId);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        Long courseId = 205L;
        when(mockedCourse.getId()).thenReturn(courseId);
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        facade.register(studentId, courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotRegister_StudentNotExists() {
        String commandId = COURSE_REGISTER;
        Long studentId = 103L;
        Long courseId = 206L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:103 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade, never()).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotRegister_CourseNotExists() {
        String commandId = COURSE_REGISTER;
        Long studentId = 104L;
        Long courseId = 207L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Exception exception = assertThrows(CourseNotExistsException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:207 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade, never()).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotRegister_StudentCoursesExceed() {
        String commandId = COURSE_REGISTER;
        Long studentId = 105L;
        Long courseId = 208L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(mockedStudent.getCourses()).thenReturn(IntStream.range(1, 10).mapToObj(i -> mockedCourse).toList());

        Exception exception = assertThrows(StudentCoursesExceedException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:105 exceeds maximum courses.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade, never()).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotRegister_NoRoomInTheCourse() {
        String commandId = COURSE_REGISTER;
        Long studentId = 106L;
        Long courseId = 209L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(mockedCourse.getStudents()).thenReturn(IntStream.range(1, 51).mapToObj(i -> mockedStudent).toList());

        Exception exception = assertThrows(NoRoomInTheCourseException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:209 does not have enough rooms.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade, never()).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldUnRegister() throws CourseNotExistsException, StudentNotExistsException {
        String commandId = COURSE_UN_REGISTER;
        Long studentId = 107L;
        Long courseId = 210L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        facade.unRegister(studentId, courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade).unLink(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotUnRegister_StudentNotExists() {
        String commandId = COURSE_UN_REGISTER;
        Long studentId = 108L;
        Long courseId = 211L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:108 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade, never()).unLink(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotUnRegister_CourseNotExists() {
        String commandId = COURSE_UN_REGISTER;
        Long studentId = 109L;
        Long courseId = 212L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Exception exception = assertThrows(CourseNotExistsException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:212 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(new Long[]{studentId, courseId});
        verify(persistenceFacade, never()).unLink(mockedStudent, mockedCourse);
    }

    private CommandsFactory buildFactory() {
        return new CourseCommandsFactory(
                List.of(
                        spy(new FindCourseCommand(persistenceFacade)),
                        spy(new FindRegisteredCoursesCommand(persistenceFacade)),
                        spy(new FindCoursesWithoutStudentsCommand(persistenceFacade)),
                        spy(new CreateOrUpdateCourseCommand(persistenceFacade)),
                        spy(new DeleteCourseCommand(persistenceFacade)),
                        spy(new RegisterStudentToCourseCommand(persistenceFacade, 50, 5)),
                        spy(new UnRegisterStudentFromCourseCommand(persistenceFacade))
                )
        );
    }

}
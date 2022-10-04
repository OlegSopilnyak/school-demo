package oleg.sopilnyak.test.service.facade.course;

import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.course.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursesFacadeImplTest {
    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    @Spy
    CommandsFactory factory = buildFactory();

    @Spy
    @InjectMocks
    CoursesFacadeImpl facade;

    @Test
    void shouldNotFindById() {
        Long courseId = 100L;

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isEmpty();
        verify(factory).command(CourseCommandsFacade.FIND_BY_ID);
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    void shouldFindById() {
        //TODO implement it later
    }

    @Test
    void shouldNotFindRegisteredFor() {
        Long studentId = 200L;

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).isEmpty();
        verify(factory).command(CourseCommandsFacade.FIND_REGISTERED);
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    void shouldNotFindWithoutStudents() {

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).isEmpty();
        verify(factory).command(CourseCommandsFacade.FIND_NOT_REGISTERED);
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    void shouldNotCreateOrUpdate() {
        Course courseToUpdate = mock(Course.class);

        Optional<Course> course = facade.createOrUpdate(courseToUpdate);

        assertThat(course).isEmpty();
        verify(factory).command(CourseCommandsFacade.CREATE_OR_UPDATE);
        verify(persistenceFacade).save(courseToUpdate);
    }

    @Test
    void shouldNotDelete() {
        Long courseId = 101L;

        CourseNotExistsException exception = assertThrows(CourseNotExistsException.class, () -> facade.delete(courseId));

        assertThat("Course with ID:101 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.DELETE);
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    void shouldNotRegister() {
        Long studentId = 202L;
        Long courseId = 102L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.register(studentId, courseId));

        assertThat("Student with ID:202 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.REGISTER);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotUnRegister() {
        Long studentId = 203L;
        Long courseId = 103L;

        Exception exception = assertThrows(StudentNotExistsException.class, () -> facade.unRegister(studentId, courseId));

        assertThat("Student with ID:203 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(CourseCommandsFacade.UN_REGISTER);
        verify(persistenceFacade, never()).unLink(any(Student.class), any(Course.class));
    }

    private CommandsFactory buildFactory() {
        return new SchoolCommandsFactory(
                Map.of(
                        CourseCommandsFacade.FIND_BY_ID, spy(new FindCourseCommand(persistenceFacade)),
                        CourseCommandsFacade.FIND_REGISTERED, spy(new FindRegisteredCoursesCommand(persistenceFacade)),
                        CourseCommandsFacade.FIND_NOT_REGISTERED, spy(new FindCoursesWithoutStudentsCommand(persistenceFacade)),
                        CourseCommandsFacade.CREATE_OR_UPDATE, spy(new CreateOrUpdateCourseCommand(persistenceFacade)),
                        CourseCommandsFacade.DELETE, spy(new DeleteCourseCommand(persistenceFacade)),
                        CourseCommandsFacade.REGISTER, spy(new RegisterStudentToCourseCommand(persistenceFacade, 50, 5)),
                        CourseCommandsFacade.UN_REGISTER, spy(new UnRegisterStudentFromCourseCommand(persistenceFacade))
                )
        );
    }

}
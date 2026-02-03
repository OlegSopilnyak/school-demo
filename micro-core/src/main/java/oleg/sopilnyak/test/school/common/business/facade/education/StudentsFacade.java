package oleg.sopilnyak.test.school.common.business.facade.education;

import oleg.sopilnyak.test.school.common.business.facade.education.base.EducationFacade;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service-Facade: Service for manage students in the school
 */
public interface StudentsFacade extends EducationFacade {
    String SUBSPACE = "::students";
    String FIND_BY_ID = NAMESPACE + SUBSPACE + ":find.By.Id";
    String FIND_ENROLLED = NAMESPACE + SUBSPACE + ":find.Enrolled.To.The.Course";
    String FIND_NOT_ENROLLED = NAMESPACE + SUBSPACE + ":find.Not.Enrolled.To.Any.Course";
    String CREATE_OR_UPDATE = NAMESPACE + SUBSPACE + ":create.Or.Update";
    String CREATE_MACRO = NAMESPACE + SUBSPACE + ":create.Macro";
    String DELETE = NAMESPACE + SUBSPACE + ":delete";
    String DELETE_MACRO = NAMESPACE + SUBSPACE + ":delete.Macro";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(
            FIND_BY_ID, FIND_ENROLLED, FIND_NOT_ENROLLED, CREATE_OR_UPDATE, CREATE_MACRO, DELETE, DELETE_MACRO
    );

    /**
     * To get the list of valid action-ids
     *
     * @return valid action-ids for concrete descendant-facade
     */
    @Override
    default List<String> validActions() {
        return ACTION_IDS;
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "StudentsFacade";
    }

    /**
     * To get the student by ID
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<Student> findById(Long id);

    /**
     * To get students enrolled to the course
     *
     * @param courseId system-id of the course
     * @return set of students
     * @deprecated
     */
    @Deprecated
    Set<Student> findEnrolledTo(Long courseId);

    /**
     * To get students enrolled to the course
     *
     * @param course course instance
     * @return set of students
     * @deprecated
     */
    @Deprecated
    default Set<Student> findEnrolledTo(Course course) {
        return isInvalid(course) ? Collections.emptySet() : findEnrolledTo(course.getId());
    }

    /**
     * To get students not enrolled to any course
     *
     * @return set of students
     * @deprecated
     */
    @Deprecated
    Set<Student> findNotEnrolled();

    /**
     * To create or update student instance
     *
     * @param student student should be created or updated
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<Student> createOrUpdate(Student student);

    /**
     * To create student instance + it's profile
     *
     * @param student student should be created
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<Student> create(Student student);

    /**
     * To delete student from the school
     *
     * @param studentId system-id of the student
     * @return true if success
     * @throws StudentNotFoundException    throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     * @deprecated
     */
    @Deprecated
    boolean delete(Long studentId) throws StudentNotFoundException, StudentWithCoursesException;

    /**
     * To delete student from the school
     *
     * @param student student instance
     * @return true if success
     * @throws StudentNotFoundException    throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     * @deprecated
     */
    @Deprecated
    default boolean delete(Student student) throws StudentNotFoundException, StudentWithCoursesException {
        return !isInvalid(student) && delete(student.getId());
    }
}

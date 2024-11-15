package oleg.sopilnyak.test.end2end.facade;

import oleg.sopilnyak.test.school.common.exception.organization.*;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.model.PersonProfile;

import java.util.Optional;
import java.util.Set;

public class PersistenceFacadeDelegate implements PersistenceFacade {
    final PersistenceFacade delegator;

    public PersistenceFacadeDelegate(PersistenceFacade persistenceFacade) {
        this.delegator = persistenceFacade;
    }

    /**
     * To initialize default minimal data-set for the application
     */
    @Override
    public void initDefaultDataset() {
        delegator.initDefaultDataset();
    }

    /**
     * To update authority person's access parameters
     *
     * @param personId system-id of authority person
     * @param username new value of login's username
     * @param password new value of login's password
     * @return true if changes applied
     */
    @Override
    public boolean updateAuthorityPersonAccess(Long personId, String username, String password) {
        return delegator.updateAuthorityPersonAccess(personId, username, password);
    }

    /**
     * To find course by id
     *
     * @param id system-id of the course
     * @return student instance or empty() if not exists
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Course> findCourseById(Long id) {
        return delegator.findCourseById(id);
    }

    /**
     * Create or update course
     *
     * @param course course instance to store
     * @return course instance or empty(), if instance couldn't store
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Course> save(Course course) {
        return delegator.save(course);
    }

    /**
     * Delete course by id
     *
     * @param courseId system-id of the course
     */
    @Override
    public void deleteCourse(Long courseId) {
        delegator.deleteCourse(courseId);
    }

    /**
     * To check is there is any course in the database<BR/>For tests purposes only
     *
     * @return true if there is no course in database
     */
    @Override
    public boolean isNoCourses() {
        return delegator.isNoCourses();
    }

    /**
     * To get all authority persons of the school
     *
     * @return the set of authority persons
     * @see AuthorityPerson
     */
    @Override
    public Set<AuthorityPerson> findAllAuthorityPersons() {
        return delegator.findAllAuthorityPersons();
    }

    /**
     * To find authority person by id
     *
     * @param id system-id of the authority person
     * @return authority person instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> findAuthorityPersonById(Long id) {
        return delegator.findAuthorityPersonById(id);
    }

    /**
     * To find authority person by profile-id
     *
     * @param id system-id of the profile of the authority person
     * @return authority person instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> findAuthorityPersonByProfileId(Long id) {
        return delegator.findAuthorityPersonByProfileId(id);
    }

    /**
     * Create or update authority person
     *
     * @param person authority person instance to store
     * @return authority person instance or empty(), if instance couldn't store
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> save(AuthorityPerson person) {
        return delegator.save(person);
    }

    /**
     * To delete authority person by id
     *
     * @param id system-id of the authority person
     * @throws AuthorityPersonManagesFacultyException throws when you want to delete authority person who is the dean of a faculty now
     * @throws AuthorityPersonNotFoundException   throws when you want to delete authority person who is not created before
     * @see AuthorityPerson
     */
    @Override
    public boolean deleteAuthorityPerson(Long id) throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        return delegator.deleteAuthorityPerson(id);
    }


    /**
     * To get all faculties of the school
     *
     * @return the set of faculties
     * @see Faculty
     */
    @Override
    public Set<Faculty> findAllFaculties() {
        return delegator.findAllFaculties();
    }

    /**
     * To find faculty by id
     *
     * @param id system-id of the faculty
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Faculty> findFacultyById(Long id) {
        return delegator.findFacultyById(id);
    }

    /**
     * Create or update faculty instance
     *
     * @param instance faculty instance to store
     * @return faculty instance or empty(), if instance couldn't store
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Faculty> save(Faculty instance) {
        return delegator.save(instance);
    }

    /**
     * To delete faculty by id
     *
     * @param id system-id of the faculty
     * @throws FacultyNotFoundException  throws when you want to delete faculty which is not created before
     * @throws FacultyIsNotEmptyException throws when you want to delete faculty which has courses
     * @see Faculty
     */
    @Override
    public void deleteFaculty(Long id) throws FacultyNotFoundException, FacultyIsNotEmptyException {
        delegator.deleteFaculty(id);
    }

    /**
     * To get all students groups of the school
     *
     * @return the set of students groups
     * @see StudentsGroup
     */
    @Override
    public Set<StudentsGroup> findAllStudentsGroups() {
        return delegator.findAllStudentsGroups();
    }

    /**
     * To find students group by id
     *
     * @param id system-id of the students group
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> findStudentsGroupById(Long id) {
        return delegator.findStudentsGroupById(id);
    }

    /**
     * Create or update students group instance
     *
     * @param instance students group instance to store
     * @return students group instance or empty(), if instance couldn't store
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> save(StudentsGroup instance) {
        return delegator.save(instance);
    }

    /**
     * To delete students group by id
     *
     * @param id system-id of the students group
     * @throws StudentsGroupNotFoundException   throws when you want to delete students group which is not created before
     * @throws StudentGroupWithStudentsException throws when you want to delete students group with students
     * @see StudentsGroup
     */
    @Override
    public void deleteStudentsGroup(Long id) throws StudentsGroupNotFoundException, StudentGroupWithStudentsException {
        delegator.deleteStudentsGroup(id);
    }

    /**
     * To find enrolled students by course-id
     *
     * @param courseId system-id of the course
     * @return set of students
     */
    @Override
    public Set<Student> findEnrolledStudentsByCourseId(Long courseId) {
        return delegator.findEnrolledStudentsByCourseId(courseId);
    }

    /**
     * To find not enrolled to any course students
     *
     * @return set of students
     */
    @Override
    public Set<Student> findNotEnrolledStudents() {
        return delegator.findNotEnrolledStudents();
    }

    /**
     * To find courses registered for student
     *
     * @param studentId system-id of student
     * @return set of courses
     */
    @Override
    public Set<Course> findCoursesRegisteredForStudent(Long studentId) {
        return delegator.findCoursesRegisteredForStudent(studentId);
    }

    /**
     * To find courses without students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findCoursesWithoutStudents() {
        return delegator.findCoursesWithoutStudents();
    }

    /**
     * To link the student with the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if linking successful
     */
    @Override
    public boolean link(Student student, Course course) {
        return delegator.link(student, course);
    }

    /**
     * To un-link the student from the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if un-linking successful
     */
    @Override
    public boolean unLink(Student student, Course course) {
        return delegator.unLink(student, course);
    }

    /**
     * To find student by id
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> findStudentById(Long id) {
        return delegator.findStudentById(id);
    }

    /**
     * Create or update student
     *
     * @param instance student instance to store
     * @return student instance or empty(), if instance couldn't store
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> save(Student instance) {
        return delegator.save(instance);
    }

    /**
     * Delete student by id
     *
     * @param studentId system-id of the student
     * @return true if student deletion successfully
     */
    @Override
    public boolean deleteStudent(Long studentId) {
        return delegator.deleteStudent(studentId);
    }

    /**
     * To check is there are any student in the database<BR/>For tests purposes only
     *
     * @return true if there is no student in database
     */
    @Override
    public boolean isNoStudents() {
        return delegator.isNoStudents();
    }

    /**
     * To get person-profile instance by login value
     *
     * @param login system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> findPersonProfileByLogin(String login) {
        return delegator.findPersonProfileByLogin(login);
    }

    /**
     * To get person-profile instance by id
     *
     * @param id system-id of the course
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> findProfileById(Long id) {
        return delegator.findProfileById(id);
    }

    /**
     * To save principal-profile instance
     *
     * @param profile instance to save
     * @return saved instance of empty() if cannot
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> saveProfile(PersonProfile profile) {
        return delegator.saveProfile(profile);
    }

    /**
     * To delete the profile by profile-id
     *
     * @param id the system-id of the profile
     */
    @Override
    public void deleteProfileById(Long id) {
        delegator.deleteProfileById(id);
    }

    /**
     * Convert profile to entity bean
     *
     * @param profile instance to convert
     * @return instance ready to use in the repository
     */
    @Override
    public <T extends PersonProfile> T toEntity(T profile) {
        return delegator.toEntity(profile);
    }
}

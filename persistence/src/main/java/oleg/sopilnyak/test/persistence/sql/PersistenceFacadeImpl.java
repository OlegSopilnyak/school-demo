package oleg.sopilnyak.test.persistence.sql;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.persistence.sql.entity.*;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.*;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service-Facade-Implementation: Service for manage persistence layer of the school
 */
@Slf4j
@Component
public class PersistenceFacadeImpl implements PersistenceFacade {
    @Autowired
    private SchoolEntityMapper mapper;
    @Resource
    private StudentRepository studentRepository;
    @Resource
    private CourseRepository courseRepository;
    @Resource
    private AuthorityPersonRepository authorityPersonRepository;
    @Resource
    private FacultyRepository facultyRepository;
    @Resource
    private StudentsGroupRepository studentsGroupRepository;

    /**
     * To initialize default minimal data-set for the application
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void initDefaultDataset() {
        StudentEntity malePupil = StudentEntity.builder()
                .firstName("John").lastName("Doe").gender("Mr")
                .description("The best male pupil in the School.")
                .build();
        if (studentExists(malePupil)) {
            log.info("Default data-set is installed already.");
            return;
        }
        StudentEntity femalePupil = StudentEntity.builder()
                .firstName("Jane").lastName("Doe").gender("Ms")
                .description("The best female pupil in the School.")
                .build();
        CourseEntity english = CourseEntity.builder()
                .name("English")
                .description("Internation language obligated for all around the world.")
                .build();
        CourseEntity mathematics = CourseEntity.builder()
                .name("Mathematics")
                .description("The queen of the sciences.")
                .build();
        CourseEntity geographic = CourseEntity.builder()
                .name("geographic")
                .description("The science about sever countries location and habits.")
                .build();

        log.info("Saving students set...");
        save(malePupil);
        save(femalePupil);

        log.info("Saving courses set...");
        save(english);
        save(mathematics);
        save(geographic);

        log.info("Linking students with courses...");
        link(malePupil, english);
        link(malePupil, mathematics);
        link(malePupil, geographic);

        link(femalePupil, english);
        link(femalePupil, mathematics);
        link(femalePupil, geographic);
    }

    /**
     * To find student by id
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> findStudentById(Long id) {
        return studentRepository.findById(id).map(student -> student);
    }

    /**
     * To find enrolled students by course-id
     *
     * @param courseId system-id of the course
     * @return set of students
     */
    @Override
    public Set<Student> findEnrolledStudentsByCourseId(Long courseId) {
        return studentRepository.findStudentEntitiesByCourseSetId(courseId)
                .stream()
                .map(student -> (Student) student)
                .collect(Collectors.toSet());
    }

    /**
     * To find not enrolled to any course students
     *
     * @return set of students
     */
    @Override
    public Set<Student> findNotEnrolledStudents() {
        return studentRepository.findStudentEntitiesByCourseSetEmpty()
                .stream()
                .map(student -> (Student) student)
                .collect(Collectors.toSet());
    }

    /**
     * Create or update student
     *
     * @param instance student instance to store
     * @return student instance or empty(), if instance couldn't store
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<Student> save(Student instance) {
        final StudentEntity entity =
                instance instanceof StudentEntity ? (StudentEntity) instance : mapper.toEntity(instance);
        return Optional.of(studentRepository.saveAndFlush(entity));
    }

    /**
     * Delete student by id
     *
     * @param studentId system-id of the student
     * @return true if student deletion successfully
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean deleteStudent(Long studentId) {
        studentRepository.deleteById(studentId);
        studentRepository.flush();
        return true;
    }

    /**
     * To find course by id
     *
     * @param id system-id of the course
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Course> findCourseById(Long id) {
        return courseRepository.findById(id).map(course -> course);
    }

    /**
     * To find courses registered for student
     *
     * @param studentId system-id of student
     * @return set of courses
     */
    @Override
    public Set<Course> findCoursesRegisteredForStudent(Long studentId) {
        return courseRepository.findCourseEntitiesByStudentSetId(studentId)
                .stream()
                .map(course -> (Course) course)
                .collect(Collectors.toSet());
    }

    /**
     * To find courses without students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findCoursesWithoutStudents() {
        return courseRepository.findCourseEntitiesByStudentSetEmpty()
                .stream()
                .map(course -> (Course) course)
                .collect(Collectors.toSet());
    }

    /**
     * Create or update course
     *
     * @param instance course instance to store
     * @return course instance or empty(), if instance couldn't store
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<Course> save(Course instance) {
        final CourseEntity entity =
                instance instanceof CourseEntity ? (CourseEntity) instance : mapper.toEntity(instance);
        return Optional.of(courseRepository.saveAndFlush(entity));
    }

    /**
     * Delete course by id
     *
     * @param courseId system-id of the course
     * @return true if the course deletion successfully
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean deleteCourse(Long courseId) {
        courseRepository.deleteById(courseId);
        courseRepository.flush();
        return true;
    }

    /**
     * To link the student with the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if linking successful
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean link(Student student, Course course) {
        final Optional<StudentEntity> studentEntity = studentRepository.findById(student.getId());
        if (studentEntity.isEmpty()) {
            return false;
        }

        final Optional<CourseEntity> courseEntity = courseRepository.findById(course.getId());
        if (courseEntity.isEmpty()) {
            return false;
        }

        if (!studentEntity.get().add(courseEntity.get())) {
            return false;
        }

        studentRepository.saveAndFlush(studentEntity.get());
        return true;
    }

    /**
     * To un-link the student from the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if un-linking successful
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean unLink(Student student, Course course) {
        final Optional<StudentEntity> studentEntity = studentRepository.findById(student.getId());
        if (studentEntity.isEmpty()) {
            return false;
        }

        final Optional<CourseEntity> courseEntity = courseRepository.findById(course.getId());
        if (courseEntity.isEmpty()) {
            return false;
        }

        if (!studentEntity.get().remove(courseEntity.get())) {
            return false;
        }

        studentRepository.saveAndFlush(studentEntity.get());
        return true;
    }

    /**
     * To get all authority persons of the school
     *
     * @return the set of authority persons
     * @see AuthorityPerson
     */
    @Override
    public Set<AuthorityPerson> findAllAuthorityPersons() {
        return authorityPersonRepository.findAll().stream()
                .map(person -> (AuthorityPerson) person)
                .collect(Collectors.toSet());
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
        return authorityPersonRepository.findById(id).map(person -> person);
    }

    /**
     * Create or update authority person
     *
     * @param instance authority person instance to store
     * @return authority person instance or empty(), if instance couldn't store
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<AuthorityPerson> save(AuthorityPerson instance) {
        final AuthorityPersonEntity entity =
                instance instanceof AuthorityPersonEntity ? (AuthorityPersonEntity) instance : mapper.toEntity(instance);
        return Optional.of(authorityPersonRepository.saveAndFlush(entity));
    }

    /**
     * To delete authority person by id
     *
     * @param id system-id of the authority person
     * @throws AuthorityPersonManageFacultyException throws when you want to delete authority person who is the dean of a faculty now
     * @throws AuthorityPersonIsNotExistsException   throws when you want to delete authority person who is not created before
     * @see AuthorityPerson
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteAuthorityPerson(Long id) throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Optional<AuthorityPersonEntity> person = authorityPersonRepository.findById(id);
        if (person.isEmpty()) {
            throw new AuthorityPersonIsNotExistsException("No authorization person with ID:" + id);
        } else if (!person.get().getFaculties().isEmpty()) {
            throw new AuthorityPersonManageFacultyException("Authorization person with ID:" + id + " is not empty");
        }
        authorityPersonRepository.deleteById(id);
        authorityPersonRepository.flush();
    }

    /**
     * To get all faculties of the school
     *
     * @return the set of faculties
     * @see Faculty
     */
    @Override
    public Set<Faculty> findAllFaculties() {
        return facultyRepository.findAll().stream()
                .map(person -> (Faculty) person)
                .collect(Collectors.toSet());
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
        return facultyRepository.findById(id).map(person -> person);
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
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<Faculty> save(Faculty instance) {
        final FacultyEntity entity =
                instance instanceof FacultyEntity ? (FacultyEntity) instance : mapper.toEntity(instance);
        return Optional.of(facultyRepository.saveAndFlush(entity));
    }

    /**
     * To delete faculty by id
     *
     * @param id system-id of the faculty
     * @throws FacultyNotExistsException  throws when you want to delete faculty which is not created before
     * @throws FacultyIsNotEmptyException throws when you want to delete faculty which has courses
     * @see Faculty
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteFaculty(Long id) throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Optional<FacultyEntity> faculty = facultyRepository.findById(id);
        if (faculty.isEmpty()) {
            throw new FacultyNotExistsException("No faculty with ID:" + id);
        } else if (!faculty.get().getCourses().isEmpty()) {
            throw new FacultyIsNotEmptyException("Faculty with ID:" + id + " is not empty");
        }
        facultyRepository.deleteById(id);
        facultyRepository.flush();
    }

    /**
     * To get all students groups of the school
     *
     * @return the set of students groups
     * @see StudentsGroup
     */
    @Override
    public Set<StudentsGroup> findAllStudentsGroups() {
        return studentsGroupRepository.findAll().stream()
                .map(group -> (StudentsGroup) group)
                .collect(Collectors.toSet());
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
        return studentsGroupRepository.findById(id).map(person -> person);
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
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<StudentsGroup> save(StudentsGroup instance) {
        final StudentsGroupEntity entity =
                instance instanceof StudentsGroupEntity ? (StudentsGroupEntity) instance : mapper.toEntity(instance);
        return Optional.of(studentsGroupRepository.saveAndFlush(entity));
    }

    /**
     * To delete students group by id
     *
     * @param id system-id of the students group
     * @throws StudentsGroupNotExistsException   throws when you want to delete students group which is not created before
     * @throws StudentGroupWithStudentsException throws when you want to delete students group with students
     * @see StudentsGroup
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteStudentsGroup(Long id) throws StudentsGroupNotExistsException, StudentGroupWithStudentsException {
        Optional<StudentsGroupEntity> faculty = studentsGroupRepository.findById(id);
        if (faculty.isEmpty()) {
            throw new StudentsGroupNotExistsException("No students group with ID:" + id);
        } else if (!faculty.get().getStudents().isEmpty()) {
            throw new StudentGroupWithStudentsException("Students group with ID:" + id + " is not empty");
        }
        studentsGroupRepository.deleteById(id);
        studentsGroupRepository.flush();
    }

    // private methods
    private boolean studentExists(StudentEntity pupil) {
        return studentRepository.findAll().stream().anyMatch(student -> theSame(student, pupil));
    }

    private boolean theSame(StudentEntity student, StudentEntity pupil) {
        return student.getFirstName().equals(pupil.getFirstName()) &&
                student.getLastName().equals(pupil.getLastName()) &&
                student.getGender().equals(pupil.getGender()) &&
                student.getDescription().equals(pupil.getDescription())
                ;
    }


}

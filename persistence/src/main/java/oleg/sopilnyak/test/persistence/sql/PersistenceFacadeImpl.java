package oleg.sopilnyak.test.persistence.sql;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.persistence.sql.entity.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.repository.CourseRepository;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
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
public class PersistenceFacadeImpl implements PersistenceFacade {
    @Resource
    private StudentRepository studentRepository;
    @Resource
    private CourseRepository courseRepository;

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
     * @param student student instance to store
     * @return student instance or empty(), if instance couldn't store
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<Student> save(Student student) {
        return Optional.of(studentRepository.saveAndFlush((StudentEntity) student));
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
        return courseRepository.findById(id).map(student -> student);
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
     * @param course course instance to store
     * @return course instance or empty(), if instance couldn't store
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<Course> save(Course course) {
        return Optional.of(courseRepository.saveAndFlush((CourseEntity) course));
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
        Optional<StudentEntity> studentEntity = studentRepository.findById(student.getId());
        if (studentEntity.isEmpty()) {
            return false;
        }
        Optional<CourseEntity> courseEntity = courseRepository.findById(course.getId());
        if (courseEntity.isEmpty()) {
            return false;
        }
        boolean success = studentEntity.get().add(courseEntity.get());
        if (!success) {
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
        Optional<StudentEntity> studentEntity = studentRepository.findById(student.getId());
        if (studentEntity.isEmpty()) {
            return false;
        }
        Optional<CourseEntity> courseEntity = courseRepository.findById(course.getId());
        if (courseEntity.isEmpty()) {
            return false;
        }
        boolean success = studentEntity.get().remove(courseEntity.get());
        if (!success) {
            return false;
        }
        studentRepository.saveAndFlush(studentEntity.get());
        return true;
    }
}

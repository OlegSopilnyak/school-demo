package oleg.sopilnyak.test.persistence.sql;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.*;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = { "school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update" })
@Rollback
class PersistenceFacadeImplTest extends MysqlTestModelFactory {
    @Autowired
    PersistenceFacade facade;

    @Test
    void shouldLoadContext() {
        assertThat(facade).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentById() {
        StudentEntity student = buildStudentEntity(null);
        Optional<Student> saved = facade.save(student);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        assertThat(id).isNotNull();

        Optional<Student> received = facade.findStudentById(id);

        assertThat(received).isNotEmpty();
        assertThat(saved.get()).isEqualTo(received.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheStudent() {
        StudentEntity student = buildStudentEntity(1);

        facade.save(student);

        assertThat(student).isEqualTo(facade.findStudentById(student.getId()).get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheStudentWithTheCourse() {
        StudentEntity student = buildStudentEntity(3);
        facade.save(student);
        CourseEntity course = buildCourseEntity(33);
        facade.save(course);
        student.add(course);

        facade.save(student);

        Optional<Student> saved = facade.findStudentById(student.getId());

        assertThat(student).isEqualTo(saved.get());
        assertThat(((StudentEntity) saved.get()).getCourseSet()).contains(course);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudent() {
        StudentEntity student = buildStudentEntity(2);
        facade.save(student);
        assertThat(student).isEqualTo(facade.findStudentById(student.getId()).get());

        boolean success = facade.deleteStudent(student.getId());

        assertThat(success).isTrue();
        assertThat(facade.findStudentById(student.getId())).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCourseById() {
        CourseEntity course = buildCourseEntity(null);
        Optional<Course> saved = facade.save(course);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        assertThat(id).isNotNull();

        Optional<Course> received = facade.findCourseById(id);

        assertThat(saved).isEqualTo(received);
        assertThat(saved.get()).isEqualTo(received.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheCourse() {
        CourseEntity course = buildCourseEntity(1);

        facade.save(course);

        assertThat(course).isEqualTo(facade.findCourseById(course.getId()).get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheCourseWithStudent() {
        CourseEntity course = buildCourseEntity(1);
        StudentEntity student = buildStudentEntity(11);
        facade.save(course);
        facade.save(student);
        course.add(student);

        facade.save(course);

        Optional<Course> received = facade.findCourseById(course.getId());
        assertThat(course).isEqualTo(received.get());
        assertThat(received.get().getStudents()).contains(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteCourse() {
        CourseEntity course = buildCourseEntity(2);
        facade.save(course);
        assertThat(course).isEqualTo(facade.findCourseById(course.getId()).get());

        boolean success = facade.deleteCourse(course.getId());

        assertThat(success).isTrue();
        assertThat(facade.findCourseById(course.getId())).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCoursesRegisteredForStudent() {
        CourseEntity course1 = buildCourseEntity(3);
        facade.save(course1);
        CourseEntity course2 = buildCourseEntity(4);
        facade.save(course2);
        StudentEntity student = buildStudentEntity(null);
        facade.save(student);
        student.add(course1);
        student.add(course2);
        facade.save(student);

        Set<Course> registered = facade.findCoursesRegisteredForStudent(student.getId());

        assertThat(registered).containsExactlyInAnyOrder(course1, course2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCoursesWithoutStudents() {
        CourseEntity course1 = buildCourseEntity(3);
        facade.save(course1);
        CourseEntity course2 = buildCourseEntity(4);
        facade.save(course2);

        Set<Course> empty = facade.findCoursesWithoutStudents();

        assertThat(empty).containsExactlyInAnyOrder(course1, course2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindEnrolledStudentsByCourseId() {
        StudentEntity student1 = buildStudentEntity(5);
        StudentEntity student2 = buildStudentEntity(6);
        facade.save(student1);
        facade.save(student2);
        CourseEntity course = buildCourseEntity(55);
        facade.save(course);
        course.add(student1);
        course.add(student2);
        facade.save(course);

        Set<Student> enrolled = facade.findEnrolledStudentsByCourseId(course.getId());

        assertThat(enrolled).containsExactlyInAnyOrder(student1, student2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindNotEnrolledStudents() {
        StudentEntity student1 = buildStudentEntity(5);
        StudentEntity student2 = buildStudentEntity(6);
        facade.save(student1);
        facade.save(student2);

        Set<Student> enrolled = facade.findNotEnrolledStudents();

        assertThat(enrolled).containsExactlyInAnyOrder(student1, student2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldLinkStudentToCourse() {
        StudentEntity student = buildStudentEntity(7);
        facade.save(student);
        CourseEntity course = buildCourseEntity(77);
        facade.save(course);

        boolean success = facade.link(student, course);

        assertThat(success).isTrue();
        Optional<Course> courseOptional = facade.findCourseById(course.getId());
        Optional<Student> studentOptional = facade.findStudentById(student.getId());
        assertThat(studentOptional.get().getCourses()).contains(course);
        assertThat(courseOptional.get().getStudents()).contains(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnlinkStudentFromTheCourse() {
        StudentEntity student = buildStudentEntity(8);
        facade.save(student);
        CourseEntity course = buildCourseEntity(88);
        facade.save(course);
        assertThat(facade.link(student, course)).isTrue();
        assertThat(student.getCourses()).contains(course);
        assertThat(course.getStudents()).contains(student);

        boolean success = facade.unLink(student, course);

        assertThat(success).isTrue();
        Optional<Course> courseOptional = facade.findCourseById(course.getId());
        Optional<Student> studentOptional = facade.findStudentById(student.getId());
        assertThat(studentOptional.get().getCourses()).isEmpty();
        assertThat(courseOptional.get().getStudents()).isEmpty();
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllAuthorityPersons() {
        int count = 10;
        Collection<AuthorityPerson> personCollection = makeAuthorityPersons(count);
        personCollection.forEach(person -> facade.save(person));

        Set<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isNotEmpty();
        assertThat(persons.size()).isEqualTo(personCollection.size());
        assertAuthorityPersonLists(toList(personCollection), toList(persons), false);
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAuthorityPersonById() {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        Optional<AuthorityPerson> saved = facade.save(person);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();

        Optional<AuthorityPerson> result = facade.findAuthorityPersonById(id);

        assertThat(result).isNotEmpty();
        assertAuthorityPersonEquals(person, result.get(), false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveAuthorityPerson() {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        Optional<AuthorityPerson> saved = facade.save(person);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        Optional<AuthorityPerson> result = facade.findAuthorityPersonById(id);
        assertThat(result).isNotEmpty();
        assertAuthorityPersonEquals(person, result.get(), false);
        AuthorityPersonEntity entity = (AuthorityPersonEntity) result.get();
        entity.setFirstName(entity.getFirstName() + "-nextVersion");

        saved = facade.save(entity);

        assertThat(saved).isNotEmpty();
        assertAuthorityPersonEquals(entity, saved.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteAuthorityPerson() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        AuthorityPerson person = makeTestAuthorityPerson(null);
        Optional<AuthorityPerson> saved = facade.save(person);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        Optional<AuthorityPerson> result = facade.findAuthorityPersonById(id);
        assertThat(result).isNotEmpty();
        assertAuthorityPersonEquals(person, result.get(), false);
        AuthorityPersonEntity entity = (AuthorityPersonEntity) result.get();
        entity.setFaculties(List.of());
        saved = facade.save(entity);
        assertThat(saved).isNotEmpty();

        facade.deleteAuthorityPerson(id);

        assertThat(facade.findAuthorityPersonById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllFaculties() {
        int count = 15;
        Collection<Faculty> facultyCollection = makeTestFaculties(count);
        facultyCollection.forEach(faculty -> facade.save(faculty));

        Set<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isNotEmpty();
        assertThat(faculties.size()).isEqualTo(facultyCollection.size());
        assertFacultyLists(toFacultyList(facultyCollection), toFacultyList(faculties), false);
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindFacultyById() {
        Faculty faculty = makeTestFaculty(null);
        Optional<Faculty> saved = facade.save(faculty);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();

        Optional<Faculty> result = facade.findFacultyById(id);

        assertThat(result).isNotEmpty();
        assertFacultyEquals(faculty, result.get(), false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveFaculty() {
        Faculty faculty = makeTestFaculty(null);
        Optional<Faculty> saved = facade.save(faculty);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        Optional<Faculty> result = facade.findFacultyById(id);
        assertThat(result).isNotEmpty();
        assertFacultyEquals(faculty, result.get(), false);
        FacultyEntity entity = (FacultyEntity) result.get();
        entity.setName(entity.getName() + "-nextVersion");

        saved = facade.save(entity);

        assertThat(saved).isNotEmpty();
        assertFacultyEquals(entity, saved.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteFaculty() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Faculty faculty = makeTestFaculty(null);
        Optional<Faculty> saved = facade.save(faculty);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        Optional<Faculty> result = facade.findFacultyById(id);
        assertThat(result).isNotEmpty();
        assertFacultyEquals(faculty, result.get(), false);
        FacultyEntity entity = (FacultyEntity) result.get();
        entity.setCourses(entity.getCourses());
        facade.save(entity);
        result = facade.findFacultyById(id);
        entity = (FacultyEntity) result.get();
        entity.setName(entity.getName() + "-nextVersion");
        entity.setCourses(List.of());
        saved = facade.save(entity);
        assertThat(saved).isNotEmpty();

        facade.deleteFaculty(id);

        assertThat(facade.findFacultyById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllStudentsGroups() {
        int count = 5;
        final Collection<StudentsGroup> studentsGroupCollection = makeStudentsGroups(count);
        studentsGroupCollection.forEach(group -> facade.save(group));

        final Set<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isNotEmpty();
        assertThat(groups.size()).isEqualTo(studentsGroupCollection.size());
        assertStudentsGroupLists(toGroupList(studentsGroupCollection), toGroupList(groups), false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentsGroupById() {
        StudentsGroup group = makeTestStudentsGroup(null);
        Optional<StudentsGroup> saved = facade.save(group);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();

        Optional<StudentsGroup> result = facade.findStudentsGroupById(id);

        assertThat(result).isNotEmpty();
        assertStudentsGroupEquals(group, result.get(), false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveStudentsGroup() {
        StudentsGroup group = makeTestStudentsGroup(null);
        Optional<StudentsGroup> saved = facade.save(group);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        Optional<StudentsGroup> result = facade.findStudentsGroupById(id);
        assertThat(result).isNotEmpty();
        assertStudentsGroupEquals(group, result.get(), false);
        StudentsGroupEntity entity = (StudentsGroupEntity) result.get();
        entity.setName(entity.getName() + "-nextVersion");

        saved = facade.save(entity);

        assertThat(saved).isNotEmpty();
        assertStudentsGroupEquals(entity, saved.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudentsGroup() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        StudentsGroup group = makeTestStudentsGroup(null);
        Optional<StudentsGroup> saved = facade.save(group);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        Optional<StudentsGroup> result = facade.findStudentsGroupById(id);
        assertThat(result).isNotEmpty();
        assertStudentsGroupEquals(group, result.get(), false);
        StudentsGroupEntity entity = (StudentsGroupEntity) result.get();
        entity.setName(entity.getName() + "-nextVersion");
        entity.setStudents(List.of());
        saved = facade.save(entity);
        assertThat(saved).isNotEmpty();

        facade.deleteStudentsGroup(id);

        assertThat(facade.findStudentsGroupById(id)).isEmpty();
    }

    @Override
    protected Collection<Faculty> makeTestFaculties(int count) {
        Collection<Faculty> faculties = super.makeTestFaculties(count);
        faculties.forEach(faculty -> {
            clearId(faculty);
            detachDean(faculty);
        });
        return faculties;
    }

    // private methods
    @Override
    protected StudentsGroup makeTestStudentsGroup(Long id) {
        StudentsGroup group = super.makeTestStudentsGroup(id);
        clearId(group);
        return group;
    }

    @Override
    protected Collection<StudentsGroup> makeStudentsGroups(int count) {
        Collection<StudentsGroup> groups = super.makeStudentsGroups(count);
        groups.forEach(this::clearId);
        return groups;
    }


    @Override
    protected StudentsGroup makeStudentsGroup(int i) {
        StudentsGroup group = super.makeStudentsGroup(i);
        clearId(group);
        return group;
    }

    @Override
    protected Faculty makeTestFaculty(Long id) {
        Faculty faculty = super.makeTestFaculty(id);
        clearId(faculty);
        detachDean(faculty);
        return faculty;
    }

    @Override
    protected Collection<Faculty> makeFaculties(int count) {
        Collection<Faculty> faculties = super.makeFaculties(count);
        faculties.forEach(this::clearId);
        return faculties;
    }

    private List<StudentsGroup> toGroupList(Collection<StudentsGroup> groups) {
        return groups.stream().sorted(Comparator.comparing(StudentsGroup::getName)).toList();
    }

    private List<Faculty> toFacultyList(Collection<Faculty> faculties) {
        return faculties.stream().sorted(Comparator.comparing(Faculty::getName)).toList();
    }

    private List<AuthorityPerson> toList(Collection<AuthorityPerson> personCollection) {
        return personCollection.stream().sorted(Comparator.comparing(AuthorityPerson::getFullName)).toList();
    }

    @Override
    protected AuthorityPerson makeTestAuthorityPerson(Long id) {
        final AuthorityPerson person = super.makeTestAuthorityPerson(id);
        clearId(person);
        return person;
    }

    @Override
    protected Collection<AuthorityPerson> makeAuthorityPersons(int count) {
        final Collection<AuthorityPerson> persons = super.makeAuthorityPersons(count);
        persons.forEach(this::clearId);
        return persons;
    }

    private static void detachDean(Faculty faculty) {
        if (faculty instanceof FakeFaculty fake) {
            fake.setDean(null);
        }
    }

    private void clearId(AuthorityPerson instance) {
        if (instance instanceof FakeAuthorityPerson person) {
            person.setId(null);
            person.getFaculties().forEach(this::clearId);
        }
    }

    private void clearId(Faculty instance) {
        if (instance instanceof FakeFaculty faculty) {
            faculty.setId(null);
            clearId(faculty.getDean());
            faculty.getCourses().forEach(this::clearId);
        }
    }

    private void clearId(StudentsGroup instance) {
        if (instance instanceof FakeStudentsGroup group) {
            group.setId(null);
            group.getStudents().forEach(this::clearId);
        }
    }

    private void clearId(Course instance) {
        if (instance instanceof FakeCourse course) {
            course.setId(null);
            course.getStudents().forEach(this::clearId);
        }
    }

    private void clearId(Student instance) {
        if (instance instanceof FakeStudent student) {
            student.setId(null);
            student.getCourses().forEach(this::clearId);
        }
    }

    private static StudentEntity buildStudentEntity(Integer counter) {
        return counter == null ? StudentEntity.builder()
                .firstName("first-name")
                .lastName("last-name")
                .gender("gender")
                .description("description")
                .build() :
                StudentEntity.builder()
                        .firstName("first-name-" + counter)
                        .lastName("last-name-" + counter)
                        .gender("gender-" + counter)
                        .description("description-" + counter)
                        .build();
    }

    private static CourseEntity buildCourseEntity(Integer counter) {
        return counter == null ? CourseEntity.builder()
                .name("course-name")
                .description("description")
                .build() :
                CourseEntity.builder()
                        .name("course-name-" + counter)
                        .description("description-" + counter)
                        .build();
    }
}

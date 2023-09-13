package oleg.sopilnyak.test.persistence.sql.mapper;

import oleg.sopilnyak.test.persistence.sql.entity.*;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest extends TestModelFactory {
    private final EntityMapper mapper = Mappers.getMapper(EntityMapper.class);

    @Test
    void shouldTransformStudentToEntity() {
        Long id = 100L;
        String firstName = "firstName";
        String lastName = "lastName";
        String gender = "gender";
        String description = "description";
        List<Course> courses = makeCourses(5);
        Student student = FakeStudent.builder()
                .id(id).firstName(firstName).lastName(lastName).gender(gender).description(description)
                .courses(courses)
                .build();

        StudentEntity entity = mapper.toEntity(student);

        assertThat(id).isEqualTo(entity.getId());
        assertThat(firstName).isEqualTo(entity.getFirstName());
        assertThat(lastName).isEqualTo(entity.getLastName());
        assertThat(gender).isEqualTo(entity.getGender());
        assertThat(description).isEqualTo(entity.getDescription());
        assertCourseLists(courses, entity.getCourses());
    }

    @Test
    void shouldTransformCourseToEntity() {
        Long id = 101L;
        String name = "courseName";
        String description = "description";
        List<Student> students = makeStudents(50);
        Course course = FakeCourse.builder()
                .id(id).name(name).description(description).students(students)
                .build();

        CourseEntity entity = mapper.toEntity(course);

        assertThat(id).isEqualTo(entity.getId());
        assertThat(name).isEqualTo(entity.getName());
        assertThat(description).isEqualTo(entity.getDescription());
        assertStudentLists(students, entity.getStudents());
    }

    @Test
    void shouldTransformAuthorityPersonToEntity() {
        Long id = 102L;
        String firstName = "Mary";
        String lastName = "Bell";
        String title = "Professor";
        String gender = "Mrs.";
        List<Faculty> faculties =
                makeFaculties(10).stream().sorted(Comparator.comparingLong(Faculty::getId)).toList();
        AuthorityPerson person = FakeAuthorityPerson.builder()
                .id(id).title(title).firstName(firstName).lastName(lastName).gender(gender)
                .faculties(faculties)
                .build();

        AuthorityPersonEntity entity = mapper.toEntity(person);

        assertThat(id).isEqualTo(entity.getId());
        assertThat(title).isEqualTo(entity.getTitle());
        assertThat(firstName).isEqualTo(entity.getFirstName());
        assertThat(lastName).isEqualTo(entity.getLastName());
        assertThat(gender).isEqualTo(entity.getGender());
        assertFacultyLists(faculties, entity.getFaculties());
    }

    @Test
    void shouldTransformFacultyToEntity() {
        Long id = 103L;
        String name = "English";
        AuthorityPerson dean = makeTestAuthorityPerson(-101L);
        List<Course> courses = makeCourses(5);

        Faculty faculty = FakeFaculty.builder()
                .id(id).name(name).dean(dean)
                .courses(courses)
                .build();

        FacultyEntity entity = mapper.toEntity(faculty);

        assertThat(id).isEqualTo(entity.getId());
        assertThat(name).isEqualTo(entity.getName());
        assertAuthorityPersonEquals(dean, entity.getDean());
        assertCourseLists(courses, entity.getCourses());
    }

    @Test
    void shouldTransformStudentsGroupToEntity() {
        Long id = 104L;
        String name = "Hawks-2020";
        Student leader = makeStudent(100);
        List<Student> students = makeStudents(20);

        StudentsGroup group = FakeStudentsGroup.builder()
                .id(id).name(name)
                .leader(leader)
                .students(students)
                .build();

        StudentsGroupEntity entity = mapper.toEntity(group);

        assertThat(id).isEqualTo(entity.getId());
        assertThat(name).isEqualTo(entity.getName());
        assertStudentEquals(leader, entity.getLeader());
        assertStudentLists(students, entity.getStudents());
    }
}
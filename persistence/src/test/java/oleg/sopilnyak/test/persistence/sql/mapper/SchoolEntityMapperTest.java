package oleg.sopilnyak.test.persistence.sql.mapper;

import oleg.sopilnyak.test.persistence.sql.entity.*;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Comparator;
import java.util.List;

class SchoolEntityMapperTest extends TestModelFactory {
    private final SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);

    @Test
    void shouldTransformAuthorityPersonToEntity() {
        Long id = 102L;
        String firstName = "Mary";
        String lastName = "Bell";
        String title = "Professor";
        String gender = "Mrs.";
        List<Faculty> faculties =
                makeFaculties(10).stream().sorted(Comparator.comparing(Faculty::getName)).toList();
        AuthorityPerson person = FakeAuthorityPerson.builder()
                .id(id).title(title).firstName(firstName).lastName(lastName).gender(gender)
                .faculties(faculties)
                .build();

        AuthorityPersonEntity entity = mapper.toEntity(person);

        assertAuthorityPersonEquals(entity, person);
    }

    @Test
    void shouldTransformCourseToEntity() {
        Long id = 101L;
        String name = "courseName";
        String description = "description";
        List<Student> students =
                makeStudents(50).stream().sorted(Comparator.comparing(Student::getFullName)).toList();
        Course course = FakeCourse.builder()
                .id(id).name(name).description(description).students(students)
                .build();

        CourseEntity entity = mapper.toEntity(course);

        assertCourseEquals(entity, course);
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

        assertFacultyEquals(entity, faculty);
    }

    @Test
    void shouldTransformPrincipalProfileToEntity() {
        Long id = 106L;
        PrincipalProfile profile = FakePrincipalProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .login("login-" + id)
                .build();

        PrincipalProfileEntity entity = mapper.toEntity(profile);

        assertProfilesEquals(entity, profile);
    }

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

        assertStudentEquals(entity, student);
    }

    @Test
    void shouldTransformStudentProfileToEntity() {
        Long id = 105L;
        StudentProfile profile = FakeStudentsProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .build();

        StudentProfileEntity entity = mapper.toEntity(profile);

        assertProfilesEquals(entity, profile);
    }

    @Test
    void shouldTransformStudentsGroupToEntity() {
        Long id = 104L;
        String name = "Hawks-2020";
        List<Student> students =
                makeStudents(20).stream().sorted(Comparator.comparing(Student::getFullName)).toList();
        Student leader = students.get(10);
        StudentsGroup group = FakeStudentsGroup.builder()
                .id(id).name(name).leader(leader)
                .students(students)
                .build();

        StudentsGroupEntity entity = mapper.toEntity(group);

        assertStudentsGroupEquals(entity, group);
    }

}
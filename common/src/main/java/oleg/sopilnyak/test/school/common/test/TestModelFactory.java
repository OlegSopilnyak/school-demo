package oleg.sopilnyak.test.school.common.test;

import lombok.Builder;
import lombok.Data;
import oleg.sopilnyak.test.school.common.model.*;
import org.assertj.core.api.Assertions;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Class-Utility: common classes and method to test model behavior
 */
public class TestModelFactory {

    protected void assertStudentLists(List<Student> expected, List<Student> result) {
        if (ObjectUtils.isEmpty(expected)) {
            Assertions.assertThat(ObjectUtils.isEmpty(result)).isTrue();
            return;
        }
        Assertions.assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertStudentEquals(expected.get(i), result.get(i)));
    }

    protected void assertCourseLists(List<Course> expected, List<Course> result) {
        if (ObjectUtils.isEmpty(expected)) {
            Assertions.assertThat(ObjectUtils.isEmpty(result)).isTrue();
            return;
        }
        Assertions.assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertCourseEquals(expected.get(i), result.get(i)));
    }

    protected void assertCourseEquals(Course expected, Course result) {
        Assertions.assertThat(expected.getId()).isEqualTo(result.getId());
        Assertions.assertThat(expected.getName()).isEqualTo(result.getName());
        Assertions.assertThat(expected.getDescription()).isEqualTo(result.getDescription());
    }

    protected void assertStudentEquals(Student expected, Student result) {
        Assertions.assertThat(expected.getId()).isEqualTo(result.getId());
        Assertions.assertThat(expected.getFirstName()).isEqualTo(result.getFirstName());
        Assertions.assertThat(expected.getLastName()).isEqualTo(result.getLastName());
        Assertions.assertThat(expected.getGender()).isEqualTo(result.getGender());
        Assertions.assertThat(expected.getDescription()).isEqualTo(result.getDescription());
        Assertions.assertThat(expected.getFullName()).isEqualTo(result.getFullName());
    }

    protected List<Course> makeCourses(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeCourse(i + 1)).toList();
    }

    protected Course makeTestCourse(Long id) {
        String name = "courseName";
        String description = "description";
        List<Student> students = makeStudents(50);
        return FakeCourse.builder()
                .id(id).name(name).description(description).students(students)
                .build();
    }

    protected Course makeCourse(int i) {
        return FakeCourse.builder()
                .id(i + 200L)
                .name("name-" + i)
                .description("description-" + i)
                .students(Collections.emptyList())
                .build();
    }

    protected List<Student> makeStudents(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeStudent(i + 1)).toList();
    }

    protected Student makeTestStudent(Long id) {
        String firstName = "firstName";
        String lastName = "lastName";
        String gender = "gender";
        String description = "description";
        List<Course> courses = makeCourses(5);
        return FakeStudent.builder()
                .id(id).firstName(firstName).lastName(lastName).gender(gender).description(description)
                .courses(courses)
                .build();
    }

    protected Student makeStudent(int i) {
        return FakeStudent.builder()
                .id(i + 200L).firstName("firstName-" + i).lastName("lastName-" + i)
                .gender("gender-" + i).description("description-" + i)
                .courses(Collections.emptyList())
                .build();
    }

    protected AuthorityPerson makeAuthorityPerson(int i) {
        return FakeAuthorityPerson.builder()
                .id(i + 300L).title("assistant-" + i)
                .firstName("firstName-" + i).lastName("lastName-" + i).gender("gender-" + i)
                .build();
    }
    protected AuthorityPerson makeTestAuthorityPerson(Long personId) {
        String firstName = "firstName";
        String lastName = "lastName";
        String gender = "gender";
        String title = "assistant";
        return FakeAuthorityPerson.builder()
                .id(personId).title(title).firstName(firstName).lastName(lastName).gender(gender)
                .build();
    }


    protected Collection<AuthorityPerson> makeAuthorityPersons(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeAuthorityPerson(i + 1))
                .sorted(Comparator.comparing(AuthorityPerson::getFullName))
                .toList();

    }

    protected void assertAuthorityPersonLists(List<AuthorityPerson> expected, List<AuthorityPerson> result) {
        if (ObjectUtils.isEmpty(expected)) {
            Assertions.assertThat(ObjectUtils.isEmpty(result)).isTrue();
            return;
        }
        Assertions.assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertAuthorityPersonEquals(expected.get(i), result.get(i)));
    }

    protected void assertAuthorityPersonEquals(AuthorityPerson expected, AuthorityPerson result) {
        Assertions.assertThat(expected.getId()).isEqualTo(result.getId());
        Assertions.assertThat(expected.getFirstName()).isEqualTo(result.getFirstName());
        Assertions.assertThat(expected.getLastName()).isEqualTo(result.getLastName());
        Assertions.assertThat(expected.getGender()).isEqualTo(result.getGender());
        Assertions.assertThat(expected.getTitle()).isEqualTo(result.getTitle());
        Assertions.assertThat(expected.getFullName()).isEqualTo(result.getFullName());
    }
    protected Faculty makeTestFaculty(Long id){
        return FakeFaculty.builder()
                .id(id).name("faculty-id-" + id)
                .dean(makeAuthorityPerson((int)(id-200)))
                .courses(makeCourses(2))
                .build();
    }
    protected Faculty makeFaculty(int i) {
        return FakeFaculty.builder()
                .id(i + 400L).name("faculty-" + i)
                .dean(makeAuthorityPerson(i))
                .courses(makeCourses(5))
                .build();
    }
    protected Collection<Faculty> makeFaculties(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeFaculty(i + 1))
                .sorted(Comparator.comparing(Faculty::getName))
                .toList();
    }
    protected void assertFacultyEquals(Faculty expected, Faculty result) {
        Assertions.assertThat(expected.getId()).isEqualTo(result.getId());
        Assertions.assertThat(expected.getName()).isEqualTo(result.getName());
        assertAuthorityPersonEquals(expected.getDean(), result.getDean());
        assertCourseLists(expected.getCourses(), result.getCourses());
    }
    protected void assertFacultyLists(List<Faculty> expected, List<Faculty> result) {
        if (ObjectUtils.isEmpty(expected)) {
            Assertions.assertThat(ObjectUtils.isEmpty(result)).isTrue();
            return;
        }
        Assertions.assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertFacultyEquals(expected.get(i), result.get(i)));
    }


    @Data
    @Builder
    protected static class FakeStudent implements Student {
        private Long id;
        private String firstName;
        private String lastName;
        private String gender;
        private String description;
        private List<Course> courses;
    }

    @Data
    @Builder
    protected static class FakeCourse implements Course {
        private Long id;
        private String name;
        private String description;
        private List<Student> students;
    }

    @Data
    @Builder
    protected static class FakeAuthorityPerson implements AuthorityPerson {
        private Long id;
        private String title;
        private String firstName;
        private String lastName;
        private String gender;
    }

    @Data
    @Builder
    protected static class FakeFaculty implements Faculty {
        private Long id;
        private String name;
        private AuthorityPerson dean;
        private List<Course> courses;
    }

    @Data
    @Builder
    protected static class FakeStudentsGroup implements StudentsGroup {
        private Long id;
        private String name;
        private Student leader;
        private List<Student> students;
    }
}

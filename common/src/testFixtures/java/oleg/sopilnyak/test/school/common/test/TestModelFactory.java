package oleg.sopilnyak.test.school.common.test;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import oleg.sopilnyak.test.school.common.model.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Class-Utility: common classes and method to test model behavior
 */
public class TestModelFactory {

    protected void assertStudentLists(List<Student> expected, List<Student> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertStudentEquals(expected.get(i), result.get(i), checkId));
    }

    private static boolean isEmptyInputParameters(Object expected, Object result) {
        if (isEmpty(expected)) {
            assertThat(isEmpty(result)).isTrue();
            return true;
        }
        return false;
    }

    protected void assertStudentLists(List<Student> expected, List<Student> result) {
        assertStudentLists(expected, result, true);
    }

    protected void assertCourseLists(List<Course> expected, List<Course> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertCourseEquals(expected.get(i), result.get(i), checkId));
    }

    protected void assertCourseLists(List<Course> expected, List<Course> result) {
        assertCourseLists(expected, result, true);
    }

    protected void assertCourseEquals(Course expected, Course result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        if (checkId) {
            assertThat(expected.getId()).isEqualTo(result.getId());
        }
        assertThat(expected.getName()).isEqualTo(result.getName());
        assertThat(expected.getDescription()).isEqualTo(result.getDescription());
    }

    protected void assertCourseEquals(Course expected, Course result) {
        assertCourseEquals(expected, result, true);
    }

    protected void assertStudentEquals(Student expected, Student result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        if (checkId) {
            assertThat(expected.getId()).isEqualTo(result.getId());
        }
        assertThat(expected.getFirstName()).isEqualTo(result.getFirstName());
        assertThat(expected.getLastName()).isEqualTo(result.getLastName());
        assertThat(expected.getGender()).isEqualTo(result.getGender());
        assertThat(expected.getDescription()).isEqualTo(result.getDescription());
        assertThat(expected.getFullName()).isEqualTo(result.getFullName());
    }

    protected void assertStudentEquals(Student expected, Student result) {
        assertStudentEquals(expected, result, true);
    }

    protected List<Course> makeCourses(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeCourse(i + 1)).toList();
    }
    protected List<Course> makeClearCourses(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeClearCourse(i + 1)).toList();
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
    protected Course makeClearCourse(int i) {
        return FakeCourse.builder()
                .id(null)
                .name("name-" + i)
                .description("description-" + i)
                .students(Collections.emptyList())
                .build();
    }

    protected Course makeClearTestCourse() {
        String name = "courseName";
        String description = "description";
        List<Student> students = makeClearStudents(50);
        return FakeCourse.builder()
                .id(null).name(name).description(description).students(students)
                .build();
    }


    protected List<Student> makeStudents(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeStudent(i + 1)).toList();
    }
    protected List<Student> makeClearStudents(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeClearStudent(i + 1)).toList();
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

    protected Student makeClearTestStudent() {
        String firstName = "firstName";
        String lastName = "lastName";
        String gender = "gender";
        String description = "description";
        List<Course> courses = makeClearCourses(5);
        return FakeStudent.builder()
                .id(null).firstName(firstName).lastName(lastName).gender(gender).description(description)
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

    protected Student makeClearStudent(int i) {
        return FakeStudent.builder()
                .id(null).firstName("firstName-" + i).lastName("lastName-" + i)
                .gender("gender-" + i).description("description-" + i)
                .courses(Collections.emptyList())
                .build();
    }

    protected AuthorityPerson makeAuthorityPerson(int i) {
        Faculty faculty = makeFacultyNoDean(0);
        return FakeAuthorityPerson.builder()
                .id(i + 300L).title("assistant-" + i)
                .firstName("firstName-" + i).lastName("lastName-" + i).gender("gender-" + i)
                .faculties(List.of(faculty))
                .build();
    }

    protected AuthorityPerson makeCleanAuthorityPerson(int i) {
        Faculty faculty = makeCleanFacultyNoDean(0);
        return FakeAuthorityPerson.builder()
                .id(null).title("assistant-" + i)
                .firstName("firstName-" + i).lastName("lastName-" + i).gender("gender-" + i)
                .faculties(List.of(faculty))
                .build();
    }

    protected AuthorityPerson makeTestAuthorityPerson(Long personId) {
        Faculty faculty = makeFacultyNoDean(1);
        String firstName = "firstName";
        String lastName = "lastName";
        String gender = "gender";
        String title = "assistant";
        return FakeAuthorityPerson.builder()
                .id(personId).title(title).firstName(firstName).lastName(lastName).gender(gender)
                .faculties(List.of(faculty))
                .build();
    }


    protected Collection<AuthorityPerson> makeAuthorityPersons(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeAuthorityPerson(i + 1))
                .sorted(Comparator.comparing(AuthorityPerson::getFullName))
                .toList();

    }

    protected void assertAuthorityPersonLists(List<AuthorityPerson> expected, List<AuthorityPerson> result) {
        assertAuthorityPersonLists(expected, result, true);
    }

    protected void assertAuthorityPersonLists(List<AuthorityPerson> expected, List<AuthorityPerson> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertAuthorityPersonEquals(expected.get(i), result.get(i), checkId));
    }

    protected void assertAuthorityPersonEquals(AuthorityPerson expected, AuthorityPerson result) {
        assertAuthorityPersonEquals(expected, result, true);
    }

    protected void assertAuthorityPersonEquals(AuthorityPerson expected, AuthorityPerson result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        if (checkId) {
            assertThat(expected.getId()).isEqualTo(result.getId());
        }
        assertThat(expected.getFirstName()).isEqualTo(result.getFirstName());
        assertThat(expected.getLastName()).isEqualTo(result.getLastName());
        assertThat(expected.getGender()).isEqualTo(result.getGender());
        assertThat(expected.getTitle()).isEqualTo(result.getTitle());
        assertThat(expected.getFullName()).isEqualTo(result.getFullName());
    }

    protected Faculty makeTestFaculty(Long id) {
        return FakeFaculty.builder()
                .id(id).name("faculty-id-" + id)
                .dean(makeAuthorityPerson((int) (isNull(id) ? 401 : id - 200)))
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

    protected Faculty makeCleanFaculty(int i) {
        return FakeFaculty.builder()
                .id(null).name("faculty-" + i)
                .dean(makeCleanAuthorityPerson(i))
                .courses(makeClearCourses(5))
                .build();
    }

    private Faculty makeFacultyNoDean(int i) {
        return FakeFaculty.builder()
                .id(i + 400L).name("faculty-" + i)
                .dean(null)
                .courses(makeCourses(6))
                .build();
    }

    private Faculty makeCleanFacultyNoDean(int i) {
        return FakeFaculty.builder()
                .id(null).name("faculty-" + i)
                .dean(null)
                .courses(makeClearCourses(6))
                .build();
    }

    protected Collection<Faculty> makeFaculties(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeFaculty(i + 1))
                .sorted(Comparator.comparing(Faculty::getName))
                .toList();
    }

    protected Collection<Faculty> makeTestFaculties(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeTestFaculty(400L + i))
                .sorted(Comparator.comparing(Faculty::getName))
                .toList();
    }

    protected void assertFacultyEquals(Faculty expected, Faculty result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        if (checkId) {
            assertThat(expected.getId()).isEqualTo(result.getId());
        }
        assertThat(expected.getName()).isEqualTo(result.getName());
        assertCourseLists(expected.getCourses(), result.getCourses(), checkId);
    }

    protected void assertFacultyEquals(Faculty expected, Faculty result) {
        assertFacultyEquals(expected, result, true);
    }

    protected void assertFacultyLists(List<Faculty> expected, List<Faculty> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertFacultyEquals(expected.get(i), result.get(i), checkId));
    }

    protected void assertFacultyLists(List<Faculty> expected, List<Faculty> result) {
        assertFacultyLists(expected, result, true);
    }

    protected StudentsGroup makeTestStudentsGroup(Long id) {
        List<Student> students = makeStudents(2);
        return FakeStudentsGroup.builder()
                .id(id).name("students-group-id-" + id)
                .leader(students.get(0))
                .students(students)
                .build();
    }

    protected StudentsGroup makeStudentsGroup(int i) {
        List<Student> students = makeStudents(5);
        return FakeStudentsGroup.builder()
                .id(i + 500L).name("students-group-" + i)
                .leader(students.get(0))
                .students(students)
                .build();
    }

    protected StudentsGroup makeCleanStudentsGroup(int i) {
        List<Student> students = makeClearStudents(5);
        return FakeStudentsGroup.builder()
                .id(null).name("students-group-" + i)
                .leader(students.get(0))
                .students(students)
                .build();
    }

    protected Collection<StudentsGroup> makeStudentsGroups(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeStudentsGroup(i + 1))
                .sorted(Comparator.comparing(StudentsGroup::getName))
                .toList();
    }

    protected void assertStudentsGroupEquals(StudentsGroup expected, StudentsGroup result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        if (checkId) {
            assertThat(expected.getId()).isEqualTo(result.getId());
        }
        assertThat(expected.getName()).isEqualTo(result.getName());
        assertStudentEquals(expected.getLeader(), result.getLeader(), checkId);
        assertStudentLists(expected.getStudents(), result.getStudents(), checkId);
    }

    protected void assertStudentsGroupEquals(StudentsGroup expected, StudentsGroup result) {
        assertStudentsGroupEquals(expected, result, true);
    }

    protected void assertStudentsGroupLists(List<StudentsGroup> expected, List<StudentsGroup> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertStudentsGroupEquals(expected.get(i), result.get(i), checkId));
    }

    protected void assertStudentsGroupLists(List<StudentsGroup> expected, List<StudentsGroup> result) {
        assertStudentsGroupLists(expected, result, true);
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
    @ToString(exclude = "faculties")
    @Builder
    protected static class FakeAuthorityPerson implements AuthorityPerson {
        private Long id;
        private String title;
        private String firstName;
        private String lastName;
        private String gender;
        List<Faculty> faculties;
    }

    @Data
    @ToString(exclude = "courses")
    @Builder
    protected static class FakeFaculty implements Faculty {
        private Long id;
        private String name;
        private AuthorityPerson dean;
        private List<Course> courses;
    }

    @Data
    @ToString(exclude = "students")
    @Builder
    protected static class FakeStudentsGroup implements StudentsGroup {
        private Long id;
        private String name;
        private Student leader;
        private List<Student> students;
    }
}

package oleg.sopilnyak.test.school.common.test;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Class-Utility: common classes and method to test model behavior
 */
@SuppressWarnings("DuplicatedCode")
public class TestModelFactory {

    protected void assertStudentLists(List<Student> expected, List<Student> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected).hasSameSizeAs(result);
        IntStream.range(0, expected.size()).forEach(i -> assertStudentEquals(expected.get(i), result.get(i), checkId));
    }

    private static boolean isEmptyInputParameters(Object actual, Object expected) {
        if (isEmpty(actual)) {
            assertThat(isEmpty(expected)).isTrue();
            return true;
        }
        if (isEmpty(expected)) {
            assertThat(isEmpty(actual)).isTrue();
            return true;
        }
        return false;
    }

    protected void assertStudentLists(List<Student> expected, List<Student> result) {
        assertStudentLists(expected, result, true);
    }

    protected void assertCourseLists(List<Course> expected, List<Course> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected).hasSameSizeAs(result);
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
        if (isCycled(expected) || isCycled(result)) {
            return;
        }
        assertStudentLists(expected.getStudents(), result.getStudents(), checkId);
    }

    private static boolean isCycled(final Course course) {
        final List<Student> students = course.getStudents();
        return !isEmpty(students) && students.stream()
                .anyMatch(student -> {
                    final List<Course> studentCourses = student.getCourses();
                    return nonNull(studentCourses) && studentCourses.contains(course);
                });
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
        if (isCycled(expected) || isCycled(result)) {
            return;
        }
        assertCourseLists(expected.getCourses(), result.getCourses(), checkId);
    }

    private static boolean isCycled(final Student student) {
        final List<Course> courses = student.getCourses();
        return !isEmpty(courses) && courses.stream()
                .anyMatch(course -> {
                    final List<Student> courseStudents = course.getStudents();
                    return nonNull(courseStudents) && courseStudents.contains(student);
                });
    }

    protected void assertStudentEquals(Student expected, Student result) {
        assertStudentEquals(expected, result, true);
    }

    protected List<Course> makeCourses(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeCourse(i + 1))
                .sorted(Comparator.comparing(Course::getName)).toList();
    }

    protected List<Course> makeClearCourses(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> makeClearCourse(i + 1))
                .sorted(Comparator.comparing(Course::getName)).toList();
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
        return IntStream.range(0, count).mapToObj(i -> makeStudent(i + 1))
                .sorted(Comparator.comparing(Student::getFullName)).toList();
    }

    protected List<Student> makeClearStudents(int count) {
        return makeClearStudents(count, 0);
    }

    protected List<Student> makeClearStudents(int count, int startFrom) {
        return IntStream.range(0, count).mapToObj(i -> makeClearStudent(i + 1 + startFrom))
                .sorted(Comparator.comparing(Student::getFullName)).toList();
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
                .id(null).profileId((long) i).firstName("firstName-" + i).lastName("lastName-" + i)
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
        return FakeAuthorityPerson.builder()
                .id(null).title("assistant-" + i)
                .firstName("firstName-" + i).lastName("lastName-" + i).gender("gender-" + i)
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

    protected StudentProfile makeStudentProfile(Long id) {
        return FakeStudentsProfile.builder()
                .id(id)
                .email("email@email")
                .phone("phone")
                .location("location")
                .photoUrl("photo-url")
                .build();
    }

    protected PrincipalProfile makePrincipalProfile(Long id) {
        return FakePrincipalProfile.builder()
                .id(id)
                .email("email@email")
                .phone("phone")
                .location("location")
                .photoUrl("photo-url")
                .username("login-" + id)
                .signature("signature-" + id)
                .role(Role.SUPPORT_STAFF)
                .build();
    }

    protected void assertAuthorityPersonLists(List<AuthorityPerson> expected, List<AuthorityPerson> result) {
        assertAuthorityPersonLists(expected, result, true);
    }

    protected void assertAuthorityPersonLists(List<AuthorityPerson> expected, List<AuthorityPerson> result, boolean checkId) {
        if (isEmptyInputParameters(expected, result)) return;
        assertThat(expected).hasSameSizeAs(result);
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

    protected void assertProfilesEquals(PrincipalProfile actual, PrincipalProfile expected) {
        assertProfilesEquals(actual, expected, true);
    }

    protected void assertProfilesEquals(PrincipalProfile actual, PrincipalProfile expected, boolean checkId) {
        assertPersonProfilesEquals(actual, expected, checkId);
        assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
        assertThat(actual.isPassword("")).isEqualTo(expected.isPassword(""));
    }

    protected void assertProfilesEquals(StudentProfile actual, StudentProfile expected) {
        assertProfilesEquals(actual, expected, true);
    }

    protected void assertProfilesEquals(StudentProfile actual, StudentProfile expected, boolean checkId) {
        assertPersonProfilesEquals(actual, expected, checkId);
    }

    protected void assertPersonProfilesEquals(PersonProfile actual, PersonProfile expected, boolean checkId) {
        if (isEmptyInputParameters(actual, expected)) return;
        if (checkId) {
            assertThat(actual.getId()).isEqualTo(expected.getId());
        }
        assertThat(actual.getPhotoUrl()).isEqualTo(expected.getPhotoUrl());
        assertThat(actual.getEmail()).isEqualTo(expected.getEmail());
        assertThat(actual.getPhone()).isEqualTo(expected.getPhone());
        assertThat(actual.getLocation()).isEqualTo(expected.getLocation());
        assertProfileExtrasEquals(actual, expected);
    }

    private static void assertProfileExtrasEquals(PersonProfile actual, PersonProfile expected) {
        Set<String> actualExtraKeys = Set.of(actual.getExtraKeys());
        Set<String> expectedExtraKeys = Set.of(expected.getExtraKeys());
        assertThat(actualExtraKeys).hasSameSizeAs(expectedExtraKeys);
        actualExtraKeys.forEach(key -> {
            assertThat(expectedExtraKeys).contains(key);
            assertThat(actual.getExtra(key)).isEqualTo(expected.getExtra(key));
        });
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
                .build();
    }

    protected Faculty makeFacultyNoDean(int i) {
        return FakeFaculty.builder()
                .id(i + 400L).name("faculty-" + i)
                .dean(null)
                .courses(makeClearCourses(6))
                .build();
    }

    protected Faculty makeCleanFacultyNoDean(int i) {
        return FakeFaculty.builder()
                .id(null).name("faculty-" + i)
                .dean(null)
                .build();
    }

    protected Collection<Faculty> makeFaculties(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeFaculty(i + 1))
                .sorted(Comparator.comparing(Faculty::getName))
                .toList();
    }

    protected Collection<Faculty> makeCleanFaculties(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeCleanFaculty(i + 1))
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
        assertThat(expected).hasSameSizeAs(result);
        IntStream.range(0, expected.size()).forEach(i -> assertFacultyEquals(expected.get(i), result.get(i), checkId));
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
        List<Student> students = makeClearStudents(5, i * 5);
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
        assertThat(expected).hasSameSizeAs(result);
        IntStream.range(0, expected.size()).forEach(i -> assertStudentsGroupEquals(expected.get(i), result.get(i), checkId));
    }

    protected void assertStudentsGroupLists(List<StudentsGroup> expected, List<StudentsGroup> result) {
        assertStudentsGroupLists(expected, result, true);
    }

    protected void assertAccessCredentialsEquals(AccessCredentials result, AccessCredentials expected) {
        assertThat(expected.getToken()).isEqualTo(result.getToken());
        assertThat(expected.getRefreshToken()).isEqualTo(result.getRefreshToken());
    }


    @Data
    @Builder
    protected static class FakeStudent implements Student {
        private Long id;
        private Long profileId;
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
        private Long profileId;
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

    @Data
    @SuperBuilder
    protected static class FakePersonProfile implements PersonProfile {
        private Long id;
        private String photoUrl;
        private String email;
        private String phone;
        private String location;

        @Override
        public String[] getExtraKeys() {
            return new String[]{"key1", "key2", "WEB"};
        }

        public Optional<String> getExtra(String key) {
            return Optional.ofNullable(key);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    protected static class FakeStudentsProfile extends FakePersonProfile implements StudentProfile {
    }

    @Setter
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    protected static class FakePrincipalProfile extends FakePersonProfile implements PrincipalProfile {
        private String username;
        private String signature;
        // principal person role in the school
        private Role role;
        // principal person permissions in the school activities
        @Builder.Default
        private Set<Permission> permissions = new HashSet<>();

        /**
         * To check is it the correct password for login
         *
         * @param password password to check
         * @return true if password is correct
         */
        @Override
        public boolean isPassword(String password) {
            return false;
        }
    }

    @Data
    @Builder
    protected static class FakeAccessCredentials implements AccessCredentials {
        // current valid token
        private String token;
        // valid token for refreshing expired one
        private String refreshToken;
        // System-ID of the model's item
        @Builder.Default
        private Long id = 0L;
    }
}

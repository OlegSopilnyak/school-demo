package oleg.sopilnyak.test.endpoint.rest;

/**
 * Constants for request mapping roots by entity types
 */
public interface RequestMappingRoot {
    String COURSES = "/courses";
    String STUDENTS = "/students";
    String REGISTER = "/register";
    String AUTHORITIES = "/authorities";
    String PROFILES = "/profiles";
    String STUDENT_PROFILES = PROFILES + "/students";
    String PRINCIPAL_PROFILES = PROFILES + "/principals";
    String FACULTIES = "/faculties";
    String STUDENT_GROUPS = "/student-groups";

    default String coursesRoot() {
        return COURSES;
    }

    default String studentsRoot() {
        return STUDENTS;
    }

    default String registerRoot() {
        return REGISTER;
    }

    default String authoritiesRoot() {
        return AUTHORITIES;
    }
    default String studentProfilesRoot(){
        return STUDENT_PROFILES;
    }
    default String principalProfilesRoot(){
        return PRINCIPAL_PROFILES;
    }
    default String facultiesRoot() {
        return FACULTIES;
    }
    default String studentGroupsRoot() {
        return STUDENT_GROUPS;
    }
}

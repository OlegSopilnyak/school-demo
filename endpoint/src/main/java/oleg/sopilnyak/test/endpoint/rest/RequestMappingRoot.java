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
    String STUDENT_PROFILES = "/profiles/students";
    String PRINCIPAL_PROFILES = "/profiles/principals";
    String FACULTIES = "/faculties";
    String STUDENT_GROUPS = "/student-groups";
}

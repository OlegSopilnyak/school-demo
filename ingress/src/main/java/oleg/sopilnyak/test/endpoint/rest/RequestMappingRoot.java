package oleg.sopilnyak.test.endpoint.rest;

import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

/**
 * Constants for request mapping roots by entity types
 */
public final class RequestMappingRoot {
    public static final String AUTHENTICATION = AuthenticationFacade.AUTH_PATH_PREFIX;
    public static final String COURSES = "/courses";
    public static final String STUDENTS = "/students";
    public static final String PRINCIPALS = "/principals";
    public static final String REGISTER = "/register";
    public static final String AUTHORITIES = "/authorities";
    public static final String PROFILES = "/profiles";
    public static final String STUDENT_PROFILES = PROFILES + STUDENTS;
    public static final String PRINCIPAL_PROFILES = PROFILES + PRINCIPALS;
    public static final String FACULTIES = "/faculties";
    public static final String STUDENT_GROUPS = "/student-groups";

    private RequestMappingRoot() {
    }
}

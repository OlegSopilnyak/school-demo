package oleg.sopilnyak.test.school.common.model;

import java.util.List;

/**
 * Model: Type for the group of courses in the school
 */
public interface Faculty {
    /**
     * To get System-ID of the courses' group
     *
     * @return the value
     */
    Long getId();

    /**
     * To get the name of the courses' group
     *
     * @return the value of group's name
     */
    String getName();

    /**
     * To get the person who's in charge of the faculty
     *
     * @return person
     */
    AuthorityPerson getDean();

    /**
     * To get the list of courses, provided by faculty
     *
     * @return list of courses
     */
    List<Course> getCourses();
}

package oleg.sopilnyak.test.school.common.model;

import java.util.List;

/**
 * Model: Type for students in the school
 *
 * @see Person
 */
public interface Student extends Person {
    /**
     * To get description of the student
     *
     * @return description value
     */
    String getDescription();

    /**
     * To get the list of courses, the student is registered
     *
     * @return list of courses
     */
    List<Course> getCourses();

    /**
     * To get full-name of the student
     *
     * @return full name value
     */
    default String getFullName() {
        return getGender() + ". " + getFirstName() + " " + getLastName();
    }
}

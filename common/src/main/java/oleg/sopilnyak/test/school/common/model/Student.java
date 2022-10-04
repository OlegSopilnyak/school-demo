package oleg.sopilnyak.test.school.common.model;

import java.util.List;

/**
 * Model: Type for students in the school
 */
public interface Student {
    /**
     * To get System-ID of the student
     *
     * @return the value
     */
    Long getId();

    /**
     * To get first-name of the student
     *
     * @return first name value
     */
    String getFirstName();

    /**
     * To get last-name of the student
     *
     * @return last name value
     */
    String getLastName();

    /**
     * To get gender of the student
     *
     * @return gender value
     */
    String getGender();

    /**
     * To get full-name of the student
     *
     * @return full name value
     */
    default String getFullName(){
        return getGender() + ". " + getFirstName() + " " +getLastName();
    }

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
}

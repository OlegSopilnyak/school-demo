package oleg.sopilnyak.test.school.common.model;

import java.util.List;

/**
 * Model: Type for courses in the school
 */
public interface Course {
    /**
     * To get System-ID of the course
     *
     * @return the value
     */
    Long getId();

    /**
     * To get the name of the course in the school
     *
     * @return name value
     */
    String getName();

    /**
     * To get description of the course in the school
     *
     * @return description value
     */
    String getDescription();

    /**
     * To get the list of students enrolled to the course
     *
     * @return list of students
     */
    List<Student> getStudents();
}

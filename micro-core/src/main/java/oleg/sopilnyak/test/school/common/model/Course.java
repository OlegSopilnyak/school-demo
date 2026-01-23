package oleg.sopilnyak.test.school.common.model;

import java.util.List;

/**
 * Model: Type for courses in the school
 *
 * @see BaseType
 */
public interface Course extends BaseType {
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

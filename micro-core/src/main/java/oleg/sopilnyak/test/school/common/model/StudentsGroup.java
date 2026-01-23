package oleg.sopilnyak.test.school.common.model;

import java.util.List;

/**
 * Model: Type for the group of students in the school
 *
 * @see BaseType
 */
public interface StudentsGroup extends BaseType {
    /**
     * To get the name of the students' group
     *
     * @return the value of group's name
     */
    String getName();

    /**
     * To get the leader of the group
     *
     * @return leader's instance
     */
    Student getLeader();

    /**
     * To get the list of students attached to the group
     *
     * @return list of students
     */
    List<Student> getStudents();
}

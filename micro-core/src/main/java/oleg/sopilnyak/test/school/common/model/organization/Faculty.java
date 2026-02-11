package oleg.sopilnyak.test.school.common.model.organization;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.school.common.model.education.Course;

import java.util.List;

/**
 * Model: Type for the group of courses in the school
 *
 * @see BaseType
 */
public interface Faculty extends BaseType {
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

package oleg.sopilnyak.test.school.common.model.base;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Student;

/**
 * Model: Type for any person in the school.<BR/>Base type of student and authority.
 *
 * @see BaseType
 * @see Student
 * @see AuthorityPerson
 */
public interface Person extends BaseType {
    /**
     * To get System-ID of the profile linked to the person
     *
     * @return the value
     * @see PersonProfile
     */
    default Long getProfileId() {
        return -1L;
    }

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
}

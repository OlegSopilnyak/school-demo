package oleg.sopilnyak.test.school.common.model;

import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;

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

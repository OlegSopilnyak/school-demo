package oleg.sopilnyak.test.school.common.model;

/**
 * Model: Type for any person in the school.<BR/>Base type of student and authority.
 *
 * @see Student
 * @see AuthorityPerson
 */
public interface Person {
    /**
     * To get System-ID of the person
     *
     * @return the value
     */
    Long getId();

    /**
     * To get System-ID of the profile linked to the person
     *
     * @return the value
     * @see PersonProfile
     */
    default Long getProfileId(){
        return -1L;
    }
    /**
     * To get the title of the authority person
     *
     * @return person's title value
     */
    String getTitle();

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

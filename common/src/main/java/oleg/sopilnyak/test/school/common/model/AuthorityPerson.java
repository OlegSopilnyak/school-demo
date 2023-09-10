package oleg.sopilnyak.test.school.common.model;

import java.util.List;

/**
 * Model: Type for the authority person in the school
 */
public interface AuthorityPerson {
    /**
     * To get System-ID of the authority person
     *
     * @return the value
     */
    Long getId();

    /**
     * To get the title of the authority person
     *
     * @return person's title value
     */
    String getTitle();

    /**
     * To get first-name of the authority person
     *
     * @return first name value
     */
    String getFirstName();

    /**
     * To get last-name of the authority person
     *
     * @return last name value
     */
    String getLastName();

    /**
     * To get gender of the authority person
     *
     * @return gender value
     */
    String getGender();

    /**
     * To get the list of faculties where person is a dean
     *
     * @return list of faculties
     */
    List<Faculty> getFaculties();

    /**
     * To get full-name of the student
     *
     * @return full name value
     */
    default String getFullName() {
        return getGender() + ". " + getFirstName() + " " + getLastName() + " (" + getTitle() + ")";
    }
}

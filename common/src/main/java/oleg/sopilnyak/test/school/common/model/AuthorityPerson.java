package oleg.sopilnyak.test.school.common.model;

import oleg.sopilnyak.test.school.common.model.base.Person;

import java.util.List;

/**
 * Model: Type for the authority person in the school
 *
 * @see Person
 */
public interface AuthorityPerson extends Person {
    /**
     * To get the title of the authority person
     *
     * @return person's title value
     */
    String getTitle();

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
